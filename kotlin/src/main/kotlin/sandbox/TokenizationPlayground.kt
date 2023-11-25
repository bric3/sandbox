/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox

import sandbox.LogStatementScorer.matchScore
import sandbox.Tokenizer.PlaceholderMatcher
import sandbox.Tokenizer.slf4jPlaceholdersMatcher
import sandbox.Tokenizer.tokenize
import sandbox.Tokenizer.wildcardMatcher
import java.io.StreamTokenizer

class TokenizationPlayground {
    companion object {
        fun String.forEachCodePoint(block: (codePoint: Int) -> Unit) {
            var offset = 0
            var codePoint: Int
            while (offset < this.length) {
                codePoint = this.codePointAt(offset)
                block.invoke(codePoint) // pass offset / code point indexes?
                offset += Character.charCount(codePoint)
            }

        }


        private fun String.streamTokenization(): List<String> {
            StreamTokenizer(this.reader()).run {
                wordChars('{'.code, '{'.code)
                wordChars('}'.code, '}'.code)
                val tokens = ArrayList<String>()

                while (nextToken() != StreamTokenizer.TT_EOF) {
                    val token = sval

                    if (token != " ") {
                        tokens.add(token)
                    }
                }
                return tokens
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            println("log message format")
            println("==================")

            "{}{} Fragment text {}={} yet 😶‍🌫️ another piece of text".run {
                streamTokenization().print()
                println("----")
                tokenize(
                    logMessageFormat = this,
                    placeholderMatcher = ::slf4jPlaceholdersMatcher
                ).print()
            }

            println("wildcard")
            println("========")

            run {
                val templateTokens = tokenize(
                    logMessageFormat = "Computing stuff key=[wildcard]*[/wildcard]",
                    placeholderMatcher = wildcardMatcher()
                )
            }

            // log.info("pass: ids={} count={} duration={}ms", ids.size(), count, dur);
            tokenizeAndScore(
                msgFormatStr = "pass: ids={} count={} duration={}ms",
                templateStr = "pass: ids=37 count=[wildcard][5290-5299][/wildcard] duration=[wildcard][59998-60001][/wildcard]ms",
            )

            // log.error("Error processing stuff foo bar id={}, id2={}", ctx.id(), ctx.id2());
            tokenizeAndScore(
                msgFormatStr = "Error processing stuff foo bar id={}, id2={}",
                templateStr = "Error processing stuff foo bar id=[wildcard][644211-644547][/wildcard], id2=[wildcard]*[/wildcard]",
            )

            // Accumulate whitespace as single token
            tokenizeAndScore(
                msgFormatStr = "Error   processing \t stuff foo bar id={}, id2={}",
                templateStr = "Error processing stuff foo bar id=[wildcard][644211-644547][/wildcard], id2=[wildcard]*[/wildcard]",
            )

            // log.warn(
            //   "Found zero or negative value duration, duration={} id={} f={} version={} id2={} sampleType={}",
            //   dur, ctx.id(), ctx.f(), ctx.version(), ctx.id2(), sampleType);
            tokenizeAndScore(
                msgFormatStr = "Found zero or negative value duration, duration={} id={} f={} version={} id2={} sampleType={}",
                templateStr = "Found zero or negative value duration, duration=[wildcard][-117639000000-0][/wildcard] id=[wildcard][158903-1000103059][/wildcard] f=[wildcard]*[/wildcard] version=[wildcard]*[/wildcard].[wildcard]*[/wildcard][wildcard]*[/wildcard] id2=[wildcard]*[/wildcard] sampleType=[wildcard]*[/wildcard][wildcard]*[/wildcard]",
            )

            // adjacent wildcards
            // BUG version=[wildcard]*[/wildcard].[wildcard]*[/wildcard][wildcard]*[/wildcard] is not properly tokenized
            tokenizeAndScore(
                msgFormatStr = "version={} id2={} sampleType={}",
                templateStr = "version=[wildcard]*[/wildcard].[wildcard]*[/wildcard][wildcard]*[/wildcard] id2=[wildcard]*[/wildcard] sampleType=[wildcard]*[/wildcard][wildcard]*[/wildcard]",
            )
        }

        private fun tokenizeAndScore(msgFormatStr: String, templateStr: String) {
            run {
                val msgFormat = tokenize(
                    logMessageFormat = msgFormatStr,
                    placeholderMatcher = ::slf4jPlaceholdersMatcher
                )
                msgFormat.print()

                val template = tokenize(
                    logMessageFormat = templateStr,
                    placeholderMatcher = wildcardMatcher()
                )
                template.print()

                // a grok value would be:
                // pass\:\s+%{data::keyvalue("=","\\[\\]")}

                println("Score: ${msgFormat.matchScore(template)}")
            }
        }

        private fun List<*>.print() {
            joinToString(prefix = "$size tokens: ", separator = ",").run(::println)
        }
    }
}

abstract class Token {
}
data class RegularTextToken(val word: CharSequence) : Token()
data object Whitespace : Token() {
    fun isWhitespace(c: Char) = c in WHITESPACE_CHARS

    // other UTF-8 delimiters NBSP, ZWS, HAIR-SPACE, THIN-SPACE, EN-SPACE, EM-SPACE, ... ?
    private const val WHITESPACE_CHARS = " \t"

}

data class SpecialCharacter(val c: Char) : Token() // special type of delimiter?
data class WildcardToken(val wildcardValue: CharSequence) : Token()
data class LogFormattingAnchor(val anchor: CharSequence) : Token()

typealias MutableTokenList = MutableList<Token>
typealias TokenList = List<Token>


object Tokenizer {
    fun interface PlaceholderMatcher {
        fun appendIfMatching(subSequence: CharSequence, tokenList: MutableTokenList): Boolean
    }

    fun tokenize(
        logMessageFormat: CharSequence,
        placeholderMatcher: PlaceholderMatcher = PlaceholderMatcher { _, _ -> false }
    ): TokenList {
        // Currently the tokenization works on char, do we need to work with
        // Unicode code points instead?
        // e.g. val delimiterCodePoints = delimiters.codePoints().toArray()

        val tokens = mutableListOf<Token>()
        var wordStart = -1
        logMessageFormat.forEachIndexed { idx, c ->
            when {
                Whitespace.isWhitespace(c) -> {
                    if (wordStart > -1) {
                        tokens.add(RegularTextToken(logMessageFormat.subSequence(wordStart, idx)))
                        wordStart = -1
                    }
                    if (tokens.lastOrNull() !is Whitespace) {
                        tokens.add(Whitespace)
                    }
                }

                else -> {
                    // not a delimiter, keep the char
                    if (wordStart < 0) {
                        wordStart = idx
                    }

                    val element = logMessageFormat.subSequence(wordStart, idx + 1)

                    if (placeholderMatcher.appendIfMatching(element, tokens)) {
                        // placeholder matched, reset the word start
                        wordStart = -1
                    }
                    if (wordStart > 0 && idx == logMessageFormat.length - 1) {
                        tokens.add(RegularTextToken(element))
                    }
                }
            }
        }

        return tokens
    }

    fun slf4jPlaceholdersMatcher(subSequence: CharSequence, tokenList: MutableTokenList): Boolean {
        val formatPlaceholders = listOf("{}")
        val matchedParamToken = formatPlaceholders.firstOrNull { subSequence.endsWith(it) }
        return if (matchedParamToken != null) {
            if (matchedParamToken.length < subSequence.length) {
                tokenList.add(
                    RegularTextToken(
                        subSequence.subSequence(
                            0,
                            subSequence.length - matchedParamToken.length
                        )
                    )
                )
                tokenList.add(LogFormattingAnchor(matchedParamToken))
            } else {
                tokenList.add(LogFormattingAnchor(subSequence))
            }

            true
        } else {
            false
        }
    }

    fun wildcardMatcher(): PlaceholderMatcher {
        return object : PlaceholderMatcher {
            private var wildCardDepth = 0
            private var outerMostStartTagIndex = -1
            private val beforeWildcard = "[wildcard]"
            private val afterWildcard = "[/wildcard]"

            override fun appendIfMatching(subSequence: CharSequence, tokenList: MutableTokenList): Boolean {
                // Apparently wildcard can be nested?!
                // Detect wildcard start tag (the subsequence might have started by something else than a wildcard)
                val indexOfStart =
                    subSequence.indexOf(beforeWildcard, outerMostStartTagIndex + beforeWildcard.length)
                if (indexOfStart != -1) {
                    wildCardDepth++

                    if (indexOfStart > 0 && wildCardDepth == 1) {
                        // there is something before the wildcard
                        outerMostStartTagIndex = indexOfStart
                        tokenList.add(RegularTextToken(subSequence.subSequence(0, indexOfStart)))
                    }
                    // continue eating the whole pattern
                    return false
                }

                // Detect wildcard end tag
                if (subSequence.endsWith(afterWildcard)) {
                    wildCardDepth--

                    if (wildCardDepth == 0) {
                        // end of wildcard
                        tokenList.add(
                            WildcardToken(
                                subSequence.subSequence(
                                    outerMostStartTagIndex + beforeWildcard.length,
                                    subSequence.length - afterWildcard.length
                                )
                            )
                        )
                        outerMostStartTagIndex = -1
                        return true
                    } else {
                        check(wildCardDepth < 0) { "negative wildcard depth" }
                    }

                    // continue eating the whole pattern until last wildcard end tag
                    return false
                }

                return false
            }
        }
    }
}

object LogStatementScorer {
    /**
     * Matches the current log format against a template.
     *
     * E.g., the following template token list should match
     *
     * | LogFormat Tokens                  | Template Tokens                               |
     * | --------------------------------- | --------------------------------------------- |
     * | RegularTextToken(word=pass:)      | RegularTextToken(word=pass:)                  |
     * | Whitespace                        | Whitespace                                    |
     * | RegularTextToken(word=ids=)       | RegularTextToken(word=ids=37)                 |
     * | LogFormattingAnchor(anchor={})    |                                               |
     * | Whitespace                        | Whitespace                                    |
     * | RegularTextToken(word=count=)     | RegularTextToken(word=count=)                 |
     * | LogFormattingAnchor(anchor={})    | WildcardToken(wildcardValue=[5290-5299])      |
     * | Whitespace                        | Whitespace                                    |
     * | RegularTextToken(word=duration=)  | RegularTextToken(word=duration=)              |
     * | LogFormattingAnchor(anchor={})    | WildcardToken(wildcardValue=[59998-60001])    |
     * | RegularTextToken(word=ms)         | RegularTextToken(word=ms)                     |
     *
     */
    fun TokenList.matchScore(templateTokens: TokenList): Int {
        val msgFormatTokens = this
        if (msgFormatTokens.isEmpty() || templateTokens.isEmpty()) {
            return 0
        }

        // TODO split on whitespace

        var msgFormatTokensIdx = 0
        var templateTokensIdx = 0
        var score = 0
        while (
            msgFormatTokensIdx < msgFormatTokens.size
            && templateTokensIdx < templateTokens.size
        ) {
            val msgFormatToken = msgFormatTokens.getOrNull(msgFormatTokensIdx) ?: break
            val templateToken = templateTokens.getOrNull(templateTokensIdx) ?: break

            if (msgFormatToken == templateToken) {
                score++
                msgFormatTokensIdx++
                templateTokensIdx++
                continue
            }

            // When the patterns are not equal, then we need to check
            // - if the msgFormat token is a regular text composed
            // - if the template token is a wildcard
            if (msgFormatToken is RegularTextToken) {
                // check if the msgFormat token is within the template text token
                // e.g., msgFormat: "duration={}ms" is in template "duration=37ms"
                if (templateToken is RegularTextToken
                    && templateToken.word.startsWith(msgFormatToken.word)) {
                    score++

                    // Then if current msgFormatToken is followed by a LogFormattingAnchor
                    var nextMsgFormatToken = msgFormatTokens.getOrNull(msgFormatTokensIdx + 1)
                    if (nextMsgFormatToken is LogFormattingAnchor) {
                        // skip the current anchor
                        msgFormatTokensIdx++
                        score++
                    }
                    // Finally, if LogFormattingAnchor is followed by a text token,
                    // checks if the template token ends with this next msgFormat token
                    // e.g.
                    nextMsgFormatToken = msgFormatTokens.getOrNull(msgFormatTokensIdx + 1)
                    if (nextMsgFormatToken is RegularTextToken
                        && templateToken.word.endsWith(nextMsgFormatToken.word)
                    ) {
                        // skip this token since it matched
                        msgFormatTokensIdx++
                        score++
                    }

                    msgFormatTokensIdx++
                    templateTokensIdx++
                    continue
                }
            }

            // check if the msgFormat token is a formatting anchor e.g. {}
            if (msgFormatToken is LogFormattingAnchor) {
                // and the template token is a wildcard then let's assume it matches
                if (templateToken is WildcardToken) {
                    // hint to skip template tokens to grab hold on the next common whitespace or regular text,
                    // e.g., eat everything until whitespace
                    // "[wildcard]*[/wildcard].[wildcard]*[/wildcard][wildcard]*[/wildcard] "
                    //  | Wildcard            || Wildcard            | Wildcard            | Whitespace
                    //                        | RegularTextToken(word=.)
                    //
                    // or
                    // "[wildcard]*[/wildcard].[wildcard]*[/wildcard][wildcard]*[/wildcard]regular_text"
                    //  | Wildcard            || Wildcard            | Wildcard            | RegularTextToken(word=regular_text)
                    //                        | RegularTextToken(word=.)
                    val nextMsgFormatToken = msgFormatTokens.getOrNull(msgFormatTokensIdx + 1)
                    if (nextMsgFormatToken is Whitespace || nextMsgFormatToken is RegularTextToken) {
                        var nextTemplateToken = templateTokens.getOrNull(templateTokensIdx + 1)
                        while (nextTemplateToken != nextMsgFormatToken) {
                            templateTokensIdx++
                            nextTemplateToken = templateTokens.getOrNull(templateTokensIdx + 1)
                        }
                    }

                    score++
                    msgFormatTokensIdx++
                    templateTokensIdx++
                    continue
                }
            }
            // Don't know how to match those tokens so abort the scoring
            break
        }

        return score
    }
}