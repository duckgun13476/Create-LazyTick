/**
 * Loader-neutral adaptive scheduling primitives.
 *
 * <p>Algorithms placed here must operate only on explicit state and primitive values. They must
 * not depend on Minecraft, Create, loader configuration, block entities, or Mixin lifecycle.
 * Version-local adapters own event classification, persistence, configuration, and application
 * of the computed interval.</p>
 *
 * <p>The intended policy distinguishes confirmed retry failure, partial/full progress, a new
 * work session, and sustained empty-idle. It must support bounded growth and gradual decay so
 * intermittent workloads can retain a learned medium polling interval.</p>
 */
package net.pinkcats.createlazytick.adaptive;
