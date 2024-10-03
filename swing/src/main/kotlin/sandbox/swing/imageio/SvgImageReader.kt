/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.swing.imageio

import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.attributes.ViewBox
import com.github.weisj.jsvg.geometry.size.FloatSize
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import java.awt.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader
import javax.imageio.ImageTypeSpecifier
import javax.imageio.stream.ImageInputStream
import kotlin.math.max
import kotlin.math.round

class SvgImageReader(svgImageReaderSpi: SvgImageReaderSpi) : ImageReader(svgImageReaderSpi) {
  private var size: FloatSize? = null
  private var jSvgDocument: SVGDocument? = null
  override fun setInput(input: Any?, seekForwardOnly: Boolean, ignoreMetadata: Boolean) {
    super.setInput(input, seekForwardOnly, ignoreMetadata)
    reset()
  }

  override fun getWidth(imageIndex: Int): Int {
    loadInfoIfNeeded()
    return size?.width?.toInt() ?: throw IOException("SVG document not loaded")
  }

  override fun getHeight(imageIndex: Int): Int {
    loadInfoIfNeeded()
    return size?.height?.toInt() ?: throw IOException("SVG document not loaded")
  }

  @Throws(IOException::class)
  private fun loadInfoIfNeeded() {
    val input = input
    if (jSvgDocument == null && input is ImageInputStream) {

      try {
        // Not using ByteArray to avoid potential humongous allocation
        ImageInputStreamAdapter(input).buffered().use {
          jSvgDocument = SVGLoader().load(it, null, LoaderContext.createDefault())?.also { svgDocument ->
            size = svgDocument.size()
          }
        }
      } catch (e: IOException) {
        // could not read the SVG document
        Logger.getLogger(SvgImageReader::class.java.name).warning { "Could not read the SVG document: $e" }
      } catch (e: IllegalStateException) {
        // could not read the SVG document
        Logger.getLogger(SvgImageReader::class.java.name).warning { "Could not read the SVG document: $e" }
      }
    }
  }

  @Throws(IOException::class)
  override fun read(imageIndex: Int, param: ImageReadParam?): BufferedImage {
    loadInfoIfNeeded()
    jSvgDocument ?: throw IOException("SVG document not loaded")

    val sourceRenderSize = param?.sourceRenderSize

    val width =  sourceRenderSize?.width?.toDouble() ?: size!!.width.toDouble()
    val height = sourceRenderSize?.height?.toDouble() ?: size!!.height.toDouble()

    // how to have an hidpi aware image?
    val bi = BufferedImage(round(width).toInt(), round(height).toInt(), BufferedImage.TYPE_INT_ARGB)
    val g = bi.createGraphics()

    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

    jSvgDocument?.render(null as? Component, g, ViewBox(
      width.toFloat(),
      height.toFloat(),
    ))

    return bi
  }

  override fun reset() {
    jSvgDocument = null
    size = null
  }

  override fun dispose() {
    reset()
  }

  override fun getNumImages(allowSearch: Boolean): Int {
    return if (jSvgDocument != null) 1 else 0
  }

  override fun getImageTypes(imageIndex: Int): Iterator<ImageTypeSpecifier> {
    return listOf<ImageTypeSpecifier>(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)).iterator()
  }

  override fun getStreamMetadata() = null

  override fun getImageMetadata(imageIndex: Int) = null

}

private class ImageInputStreamAdapter(private val imageInputStream: ImageInputStream) : InputStream() {
  private var closed = false

  @Throws(IOException::class)
  override fun close() {
    closed = true
  }

  @Throws(IOException::class)
  override fun read(): Int {
    if(closed) throw IOException("stream closed")
    return imageInputStream.read()
  }

  @Throws(IOException::class)
  override fun read(b: ByteArray, off: Int, len: Int): Int {
    if(closed) throw IOException("stream closed")
    if (len <= 0) {
      return 0
    }
    return imageInputStream.read(b, off, max(len.toLong(), 0).toInt())
  }

  @Throws(IOException::class)
  override fun skip(n: Long): Long {
    if(closed) throw IOException("stream closed")
    if (n <= 0) {
      return 0
    }
    return imageInputStream.skipBytes(n)
  }
}