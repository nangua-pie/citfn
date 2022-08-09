/**
 * Option shapes for Vuetify 3.0.0-beta.0 selects.
 *
 * The beta release has no `item-title` / `item-value` props. Its `genItem`
 * helper in the shipped bundle reads `title` and `value` straight off each
 * option, so an object array works at runtime — but the published `SelectItem`
 * type only admits primitives and rejects it.
 *
 * Routing every option list through `selectOptions` keeps that cast in one
 * documented place, so the rest of the app keeps full type checking.
 */

import type { VSelect } from 'vuetify/components'

export interface SelectOption {
  title: string
  value: unknown
}

/**
 * The exact `items` prop type this Vuetify build declares.
 *
 * Reading it off the component keeps the binding type-checkable, where a bare
 * `never` would only be widened back to `unknown` by the template checker.
 */
export type SelectItems = NonNullable<InstanceType<typeof VSelect>['$props']['items']>

/**
 * Returns the option array ready to bind to `:items`.
 *
 * The two-step cast is the whole point of this helper. This release's
 * `SelectItem` type only admits primitives, even though its own `genItem`
 * helper reads exactly these two keys off each option at runtime. TypeScript
 * cannot see that, so the options go through `unknown` once, here, instead of
 * at every select in the app.
 */
export function selectOptions(options: SelectOption[]): SelectItems {
  return options as unknown as SelectItems
}
