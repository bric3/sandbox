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

import sandbox.TokenizationPlayground.Companion.PlaceholderMatcher
import java.io.StreamTokenizer

typealias Token = String
typealias MutableTokenList = MutableList<Token>
typealias TokenList = List<Token>

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


        private fun String.streamTokenization() : List<String> {
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

        fun interface PlaceholderMatcher {
            fun match(subSequence: CharSequence, tokenList: MutableTokenList): Boolean
        }

        fun tokenizeMessageFormat(
            logMessageFormat : CharSequence,
            placeholderMatcher: PlaceholderMatcher = PlaceholderMatcher { _,_ -> false }
        ): TokenList {
            val delimiters = " " // other delimiters?

            // Currently the tokenization works on char, do we need to work with
            // Unicode code points instead?
            // e.g. val delimiterCodePoints = delimiters.codePoints().toArray()

            val tokens = mutableListOf<String>()
            var wordStart = -1
            logMessageFormat.forEachIndexed { idx, c ->
                if (!delimiters.contains(c)) {
                    // not a delimiter, keep the char
                    if (wordStart < 0) {
                        wordStart = idx
                    }
                    val element = logMessageFormat.subSequence(wordStart, idx + 1)

                    if (placeholderMatcher.match(element, tokens)) {
                        // placeholder matched, reset the word start
                        wordStart = -1
                    }
                    if (wordStart > 0 && idx == logMessageFormat.length - 1) {
                        tokens.add(element.toString())
                    }
                } else {
                    // whitespace or any delimiter, drop the char
                    if (wordStart > -1) {
                        tokens.add(logMessageFormat.substring(wordStart, idx))
                        wordStart = -1
                    }
                }
            }

            return tokens
        }

        @JvmStatic
        fun main(args: Array<String>) {
            "{}{} Fragment text {}={} yet 😶‍🌫️ another piece of text".run {
                streamTokenization().forEach { println(it) }
                println("----")
                tokenizeMessageFormat(
                    logMessageFormat = this,
                    placeholderMatcher = ::slf4jPlaceholdersMatcher
                ).forEach { println(it) }
            }
        }

        private fun slf4jPlaceholdersMatcher(subSequence: CharSequence, tokenList: MutableTokenList): Boolean {
            val formatPlaceholders = listOf("{}")
            val matchedParamToken = formatPlaceholders.firstOrNull { subSequence.endsWith(it) }
            return if (matchedParamToken != null) {
                if (matchedParamToken.length < subSequence.length) {
                    tokenList.add(subSequence.substring(0, subSequence.length - matchedParamToken.length))
                    tokenList.add(matchedParamToken)
                } else {
                    tokenList.add(subSequence.toString())
                }

                true
            } else {
                false
            }
        }
    }
}