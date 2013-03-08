package com.pongr.jetray

import java.util.Properties
import java.io.{ File, InputStream, FileInputStream }

/** Creates an object from properties, where the properties can be obtained from multiple sources. */
trait FromProperties[T] {
  /** Reads from the specified resource file. Uses getClass.getResourceAsStream
   *  to read the file. */
  def fromResource(name: String): T = {
    val stream = getClass.getResourceAsStream(name)
    if (stream == null)
      throw new IllegalArgumentException("Resource " + name + " not found")
    fromInputStream(stream)
  }

  /** Reads from the specified file. */
  def fromFile(name: String): T = {
    val file = new File(name)
    if (!file.exists)
      throw new IllegalArgumentException("File " + name + " not found")
    fromInputStream(new FileInputStream(file))
  }

  /** Reads from the specified input stream. */
  def fromInputStream(stream: InputStream): T = {
    val props = new Properties
    props.load(stream)
    fromProperties(props)
  }

  /** Creates the desired object from the specified Properties object. */
  def fromProperties(props: Properties): T
  
  def option(s: String) = if (s != null && s.trim.length > 0) Some(s) else None
}