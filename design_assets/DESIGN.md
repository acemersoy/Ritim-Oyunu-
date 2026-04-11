# Design System Strategy: Neon Kineticism

## 1. Overview & Creative North Star
This design system is built upon the Creative North Star of **"The Digital Pulse."** It moves beyond static interfaces to create an environment that feels alive, rhythmic, and high-octane. By merging the brutalism of brushed metal with the ethereal glow of holographic projection, the system creates a high-contrast, premium experience that feels like a piece of sophisticated hardware.

The interface breaks traditional "flat" web templates by utilizing **intentional asymmetry** and **overlapping planes**. Elements should never feel like they are just "on" the screen; they should feel integrated into a deep, atmospheric space where light (Neon Cyan and Electric Purple) acts as the primary navigator.

## 2. Colors & Surface Philosophy
The palette is rooted in deep, cosmic purples and blacks, punctuated by high-energy neon signals.

*   **Primary Core:** `primary` (#50e1f9) and `primary_container` (#00bcd4) represent the "power source" of the UI.
*   **Secondary Surge:** `secondary` (#c47fff) provides the rhythmic counterpoint, used for progression and flair.
*   **Tertiary Highlight:** `tertiary` (#ffe792) is reserved for "Legendary" or high-value achievements.

### The "No-Line" Rule
To maintain a premium, cinematic feel, **1px solid borders for sectioning are strictly prohibited.** Boundaries must be defined through:
1.  **Background Shifts:** A `surface_container_low` card sitting on a `surface` background.
2.  **Glow Diffusion:** Using a soft outer glow of the `primary` color to define the edge of a high-priority element.
3.  **Tonal Transitions:** Subtle linear gradients (15% opacity) that fade into the background.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of semi-transparent holographic plates.
*   **Base Layer:** `surface` (#150529) — The deep void.
*   **Secondary Layer:** `surface_container_low` — For non-interactive layout grouping.
*   **Interactive Layer:** `surface_container_high` — For cards and active components.
*   **The Glass Rule:** Any floating overlay (modals, tooltips) must use `surface_bright` with a **backdrop-blur (20px-40px)** and a 10% opacity white "specular" top edge to simulate glass.

## 3. Typography: The Editorial Voice
The typography is a dialogue between futuristic precision and human-centric readability.

*   **Display & Headlines:** **Space Grotesk.** This font brings a technical, wide-stanced authority. Use `display-lg` for high-impact moments. Its geometric construction mirrors the "metallic" theme.
*   **Body & Utility:** **Manrope.** Chosen for its ultra-clean legibility against dark, high-contrast backgrounds. It stays "quiet" while the headlines "scream."

**Visual Hierarchy Note:** Use `on_surface_variant` (#b7a3cf) for secondary metadata to ensure the neon elements remain the focal point of the ocular journey.

## 4. Elevation & Depth
In this system, depth is a product of light, not shadows.

*   **The Layering Principle:** Avoid drop shadows. Instead, use **Tonal Layering**. Place a `surface_container_lowest` (#000000) element inside a `surface_container_high` card to create a "recessed" or "carved" look.
*   **Ambient Glows:** When an element needs to "float," use a shadow tinted with `surface_tint` (#50e1f9) at 4% opacity with a blur of 32px. This mimics the light reflection on a brushed metal surface.
*   **The "Ghost Border" Fallback:** If a container requires definition in a crowded space, use `outline_variant` (#514067) at **15% opacity**. This creates a "hairline" suggestion of a border without breaking the atmospheric immersion.

## 5. Components

### Buttons (The Kinetic Triggers)
*   **Primary:** Solid `primary_container` gradient to `primary`. Must feature an **inner glow** (1px, 20% white) on the top edge and an **outer glow** (8px blur, `primary` at 30% opacity).
*   **Secondary (Metallic):** Background of `surface_container_highest` with a brushed metal texture overlay. Text in `primary`.
*   **States:** On `Hover`, increase the `outer glow` spread by 4px. On `Press`, scale the component to 98%.

### Progress Bars (Flowing Energy)
*   **Track:** `surface_container_lowest`.
*   **Indicator:** A multi-stop linear gradient from `primary` to `secondary`. 
*   **Effect:** Add a "holographic sweep" (a white 10% opacity glare) that animates across the bar every 3 seconds to indicate active life.

### Cards (Holographic Modules)
*   **Visual:** `surface_container_high` at 80% opacity with a `backdrop-blur`.
*   **Edge:** A 1px "brushed metal" gradient border (only on the top and left sides) to simulate directional light hitting a metallic edge.
*   **Separation:** Forbid dividers. Use `spacing-6` (1.3rem) to create clear content groups.

### Inputs & Fields
*   **Container:** `surface_container_lowest` with a bottom-only `primary` border (2px).
*   **Focus State:** The entire container glows with a 10% `primary` wash.

## 6. Do's and Don'ts

### Do:
*   **Use Neon Sparingly:** Neon Cyan should represent "Action" or "Life." If everything glows, nothing is important.
*   **Embrace Asymmetry:** Offset your headers or use overlapping imagery to break the "boxed-in" feel.
*   **Layer Textures:** Apply a faint "noise" or "brushed" SVG texture to `surface_bright` elements to increase the premium feel.

### Don't:
*   **Don't use 100% White:** Use `on_background` (#f0dfff). Pure white (#FFFFFF) is too harsh and breaks the dark-room immersion.
*   **Don't use standard shadows:** Never use `rgba(0,0,0,0.5)`. Always tint your shadows with the background purple or the accent cyan.
*   **Don't use Sharp Corners:** Follow the `DEFAULT` (0.5rem) roundedness scale. Sharp corners feel "dated tech"; rounded corners feel "modern luxury tech."