/*
 * Copyright (c) 2008. All rights reserved.
 */
package ro.isdc.wro.model.resource.processor.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.processor.ResourcePostProcessor;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.util.WroUtil;


/**
 * Removes multi line comments from processed resource.
 *
 * @author Alex Objelean
 * @created Created on Nov 28, 2008
 */
public class MultiLineCommentStripperProcessor
  implements ResourcePreProcessor, ResourcePostProcessor {
  /**
   * Pattern containing a regex matching multiline comments & empty new lines.
   */
  public static final Pattern PATTERN = Pattern.compile("(?ims)[\\t ]*/\\*.*?\\*/[\\r\\n]?");
  //(\\/\\*[\\w\\'\\s\\r\\n\\*]*\\*\\/)|(\\/\\/[\\w\\s\\']*)|(\\<![\\-\\-\\s\\w\\>\\/]*\\>)
  //public static final Pattern PATTERN = Pattern.compile("(?ims)(?:\\/\\*[\\w\\'\\s\\r\\n\\*]*\\*\\/)|(\\/\\/[\\w\\s\\']*)|(\\<![\\-\\-\\s\\w\\>\\/]*\\>)");

  public static final String ALIAS = "multilineStripper";


  /**
   * {@inheritDoc}
   */
  public void process(final Resource resource, final Reader source, final Writer destination)
    throws IOException {
    try {
      final String content = IOUtils.toString(source);
      String result = PATTERN.matcher(content).replaceAll("");
      result = WroUtil.EMTPY_LINE_PATTERN.matcher(result).replaceAll("");
      destination.write(result);
    } finally {
      source.close();
      destination.close();
    }
  }


  /**
   * {@inheritDoc}
   */
  public void process(final Reader reader, final Writer writer)
    throws IOException {
    // resourceUri doesn't matter
    process(null, reader, writer);
  }
}
