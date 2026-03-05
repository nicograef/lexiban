import "reveal.js/dist/reset.css";
import "reveal.js/dist/reveal.css";
import "./theme-lexware-light.css";
import "reveal.js/plugin/highlight/monokai.css";

import Reveal from "reveal.js";
import Markdown from "reveal.js/plugin/markdown/markdown.esm.js";
import Highlight from "reveal.js/plugin/highlight/highlight.esm.js";
import Notes from "reveal.js/plugin/notes/notes.esm.js";
import mermaid from "mermaid";

(async function init() {
  const deck = new Reveal({
    hash: true,
    progress: false,
    controls: false,
    width: 1200,
    height: 700,
    margin: 0.04,
    plugins: [Markdown, Highlight, Notes],
  });

  await deck.initialize();

  // Render mermaid diagrams after Reveal is ready
  mermaid.initialize({
    startOnLoad: false,
    theme: "base",
    securityLevel: "loose",
    fontFamily: "Urbanist, sans-serif",
    themeVariables: {
      primaryColor: "#f3f1f1",
      primaryTextColor: "#131010",
      primaryBorderColor: "#ff4554",
      lineColor: "#ff4554",
      secondaryColor: "#eae6e6",
      tertiaryColor: "#fcfafa",
    },
    flowchart: { useMaxWidth: true },
    sequence: { useMaxWidth: true },
  });

  const elements = document.querySelectorAll(".mermaid");
  for (let i = 0; i < elements.length; i++) {
    const el = elements[i];
    const graphDefinition = el.textContent.trim();
    const { svg } = await mermaid.render("mermaid-" + i, graphDefinition);
    el.innerHTML = svg;
  }

  // Recalculate layout after SVGs are injected
  deck.layout();
})();
