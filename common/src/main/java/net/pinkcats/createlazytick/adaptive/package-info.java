/**
 * Loader-neutral adaptive scheduling primitives.
 *
 * <p>Algorithms placed here must operate only on explicit state and primitive values. They must
 * not depend on Minecraft, Create, loader configuration, block entities, or Mixin lifecycle.
 * Version-local adapters own event classification, persistence, configuration, and application
 * of the computed interval.</p>
 *
 * <p>Each machine owns a dedicated subpackage because its valid events, wake conditions, and
 * liveness requirements differ. Shared mathematical primitives should be extracted here only
 * after two machine functions demonstrably need the same rule.</p>
 */
package net.pinkcats.createlazytick.adaptive;
