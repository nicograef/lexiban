import "reveal.js/dist/reset.css";
import "reveal.js/dist/reveal.css";
import "reveal.js/dist/theme/dracula.css";
import "reveal.js/plugin/highlight/monokai.css";

import Reveal from "reveal.js";
import Markdown from "reveal.js/plugin/markdown/markdown.esm.js";
import Highlight from "reveal.js/plugin/highlight/highlight.esm.js";
import Notes from "reveal.js/plugin/notes/notes.esm.js";
import mermaid from "mermaid";

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
  theme: "dark",
  securityLevel: "loose",
  fontFamily: "inherit",
  flowchart: { useMaxWidth: true },
  sequence: { useMaxWidth: true },
});

const elements = document.querySelectorAll(".mermaid");
for (let i = 0; i < elements.length; i++) {
  const el = elements[i];
  const graphDefinition = el.textContent.trim();
  try {
    const { svg } = await mermaid.render(`mermaid-${i}`, graphDefinition);
    el.innerHTML = svg;
  } catch (err) {
    console.error(`Mermaid render error for diagram ${i}:`, err);
  }
}

// Recalculate layout after SVGs are injected
deck.layout();
