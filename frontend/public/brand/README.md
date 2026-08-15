# Brand Assets

PegelHub bundles its product and partner marks so the application shell has no runtime dependency
on third-party asset hosts. Angular copies this directory into the application output and serves the
files under `/brand/`.

## Inventory

- `pegelhub-logo.png`: PegelHub wordmark used in the application toolbar.
- `pegelhub-mark.svg`: compact PegelHub mark used as the preferred browser favicon.
- `viadonau-logo.svg`: partner mark used in the toolbar, retrieved unchanged from the
  [official viadonau asset](https://www.viadonau.org/_assets/28b34c7b2f06e1ac15b2ce6d7f3727c8/Images/viadonau-logo.svg)
  on 2026-07-30.
- `dhk-logo.jpg`: partner mark used in the toolbar, retrieved unchanged on 2026-07-30 from the
  official BMIMI page's `/dam/jcr:65ed57f8-b98c-45b5-8297-3dd2a003a88d/dhk.jpg` asset. The
  [DHK page](https://www.bmimi.gv.at/themen/verkehr/wasser/hochwasserschutz/3-saeulen-modell/dhk/definition.html)
  is the authoritative source page.

## Maintenance

- Reference bundled assets with a URL under `/brand/`; do not hotlink the upstream files at
  runtime.
- Preserve partner marks, colors, and aspect ratios. Layout-specific sizing belongs in component
  markup or styles, not in edited copies of the artwork.
- Replace a partner asset only from an authoritative source. Record the source and retrieval date
  here in the same change.
- Treat the committed files as the versions used by this application. This directory does not
  establish ownership of the marks or preserve editable source artwork.
- Verify the application shell at narrow and wide viewports after changing an asset, including
  accessible names and contrast in light and dark themes.
