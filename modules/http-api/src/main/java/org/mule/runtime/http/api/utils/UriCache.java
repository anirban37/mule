/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

/**
 * Cache to avoid recalculating URIs more than once
 */
public class UriCache {

  private static final int MAX_CACHE_SIZE = 2000;
  private static UriCache instance;

  private Cache<String, URI> cache = CacheBuilder.<String, String>newBuilder().maximumSize(MAX_CACHE_SIZE).build();

  private UriCache() {}

  public static UriCache getInstance() {
    if (instance == null) {
      instance = new UriCache();
    }
    return instance;
  }

  public URI getUriFromString(String uri) {
    try {
      return this.cache.get(uri, () -> URI.create(uri));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create URI for " + uri, e));
    }
  }
}
