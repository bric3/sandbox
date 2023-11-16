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

        private fun String.tokenize(): List<String> {
            val delimiters = " " // other delimiters?
            val parameterTokens = listOf("{}") // TODO may need higher lever matcher (e.g. for {0}, {1}, or else)

            // need to split on string like "{}" as a single word
            // e.g. "{}{}" becomes two tokens "{}", "{}"

            val delimiterCodePoints = delimiters.codePoints().toArray()

            val tokens = mutableListOf<String>()
            var wordStart = -1
            forEachIndexed { idx, c ->
                if (!delimiters.contains(c)) {
                    // not a delimiter, keep the char
                    if (wordStart < 0) {
                        wordStart = idx
                    }
                    val element = substring(wordStart, idx + 1)
                    val matchedParamToken = parameterTokens.firstOrNull { element.endsWith(it) }
                    if (matchedParamToken != null) {
                        if (matchedParamToken.length < element.length) {
                            tokens.add(element.substring(0, element.length - matchedParamToken.length))
                            tokens.add(matchedParamToken)
                        } else {
                            tokens.add(element)
                        }

                        wordStart = -1
                    }
                    if (wordStart >-0 && idx == length - 1) {
                        tokens.add(element)
                    }
                } else {
                    // whitespace or any delimiter, drop the char
                    if (wordStart > -1) {
                        tokens.add(substring(wordStart, idx))
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
                tokenize().forEach { println(it) }
            }
        }
    }
}