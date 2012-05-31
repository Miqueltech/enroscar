package com.stanfy;

import com.stanfy.views.BuildConfig;

/**
 * Debug flags. They are switched according to build configuration.
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 */
public final class DebugFlags {

  /** Private. */
  private DebugFlags() { /* hidden */ }

  /** Debug buffers flag. */
  public static final boolean STRICT_MODE = true; // @debug.strict@

  /** Debug buffers flag. */
  public static final boolean DEBUG_BUFFERS = false; // @debug.buffers@
  /** DB utilities debug flag. */
  public static final boolean DEBUG_DB_UTILS = false; // @debug.dbutils@

  /** IO debug flag. */
  public static final boolean DEBUG_IO = false; // @debug.io@

  /** Images debug flag. */
  public static final boolean DEBUG_IMAGES = false; // @debug.images@

  /** GUI debug flag. */
  public static final boolean DEBUG_GUI = BuildConfig.DEBUG; // @debug.gui@

  /** API debug flag. */
  public static final boolean DEBUG_API = BuildConfig.DEBUG; // @debug.api@

  /** Utilities debug flag. */
  public static final boolean DEBUG_UTILS = false; // @debug.utils@

  /** Debug service flag. */
  public static final boolean DEBUG_SERVICES = BuildConfig.DEBUG; // @debug.services@

  /** Debug parser flag. */
  public static final boolean DEBUG_PARSER = false; // @debug.parser@

  /** Debug location flag. */
  public static final boolean DEBUG_LOCATION = BuildConfig.DEBUG; // @debug.location@

  /** Debug C2DM. */
  public static final boolean DEBUG_C2DM = BuildConfig.DEBUG; // @debug.c2dm@

  /** Debug stats. */
  public static final boolean DEBUG_STATS = BuildConfig.DEBUG; // @debug.stats@

  /** Debug beans container. */
  public static final boolean DEBUG_BEANS = true; // @debug.beans@

}
