// Quiz App — plain vanilla JS
// Loads questions from questions.json, shuffles questions & options,
// renders one at a time, lets the user pick an answer and shows the explanation.

/**
 * Raw question from JSON.
 * @typedef {{id:number, question:string, options:string[], answer:number, explanation:string}} RawQuestion
 */

/**
 * Shuffled question ready for rendering.
 * options are in shuffled order, answer is the remapped index.
 * @typedef {{id:number, question:string, options:string[], answer:number, explanation:string}} ShuffledQuestion
 */

/** @type {ShuffledQuestion[]} */
let questions = [];
let currentIndex = 0;

// Per-question state: which option index the user selected, and whether checked
/** @type {Map<number, {selected: number|null, checked: boolean}>} */
const state = new Map();

// DOM refs
const questionArea = document.getElementById("question-area");
const progressText = document.getElementById("progress-text");
const progressFill = document.getElementById("progress-fill");
const btnPrev = document.getElementById("btn-prev");
const btnNext = document.getElementById("btn-next");
const btnCheck = document.getElementById("btn-check");

// ─── Shuffle helpers ─────────────────────────────────────────

/** Fisher-Yates shuffle (in-place, returns same array). */
function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/**
 * Shuffle options and remap the answer index accordingly.
 * @param {RawQuestion} q
 * @returns {ShuffledQuestion}
 */
function shuffleQuestion(q) {
  // Build index array [0,1,2,3], shuffle it, use it to reorder options
  const indices = q.options.map((_, i) => i);
  shuffle(indices);

  const shuffledOptions = indices.map((i) => q.options[i]);
  const newAnswerIndex = indices.indexOf(q.answer);

  return {
    id: q.id,
    question: q.question,
    options: shuffledOptions,
    answer: newAnswerIndex,
    explanation: q.explanation,
  };
}

// ─── Bootstrap ───────────────────────────────────────────────
async function init() {
  const res = await fetch("questions.json");
  /** @type {RawQuestion[]} */
  const raw = await res.json();

  // Shuffle question order, then shuffle each question's options
  questions = shuffle([...raw]).map(shuffleQuestion);
  render();
}

// ─── State helpers ───────────────────────────────────────────
function getState(idx) {
  if (!state.has(idx)) {
    state.set(idx, { selected: null, checked: false });
  }
  return state.get(idx);
}

const LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

// ─── Render ──────────────────────────────────────────────────
function render() {
  const q = questions[currentIndex];
  const s = getState(currentIndex);

  // Progress
  progressText.textContent = `Frage ${currentIndex + 1} / ${questions.length}`;
  progressFill.style.width = `${((currentIndex + 1) / questions.length) * 100}%`;

  // Navigation
  btnPrev.disabled = currentIndex === 0;
  btnNext.disabled = currentIndex === questions.length - 1;
  btnCheck.disabled = s.checked;

  // Question card
  let html = `<div class="question-card">`;
  html += `<div class="question-number">Frage ${currentIndex + 1}</div>`;
  html += `<div class="question-text">${escapeHtml(q.question)}</div>`;
  html += `<ul class="options-list">`;

  q.options.forEach((text, idx) => {
    const letter = LETTERS[idx];
    let classes = "option-btn";
    if (s.selected === idx) classes += " selected";

    if (s.checked) {
      const isCorrect = idx === q.answer;
      const wasSelected = s.selected === idx;
      if (wasSelected && isCorrect) classes += " correct";
      else if (wasSelected && !isCorrect) classes += " incorrect";
      else if (!wasSelected && isCorrect) classes += " missed";
    }

    const disabled = s.checked ? "disabled" : "";
    html += `<button class="${classes}" data-idx="${idx}" ${disabled}>`;
    html += `<span class="option-letter">${letter}</span>`;
    html += `<span class="option-text">${escapeHtml(text)}</span>`;
    html += `</button>`;
  });

  html += `</ul>`;

  // Explanation (only after check)
  if (s.checked) {
    const isCorrect = s.selected === q.answer;
    const correctLetter = LETTERS[q.answer];

    const resultClass = isCorrect ? "correct-result" : "incorrect-result";
    const headerClass = isCorrect ? "correct-text" : "incorrect-text";
    const headerText = isCorrect
      ? "✓ Richtig!"
      : `✗ Falsch — Richtige Antwort: ${correctLetter}`;

    html += `<div class="explanation ${resultClass}">`;
    html += `<div class="explanation-header ${headerClass}">${headerText}</div>`;
    html += `<div class="explanation-body">${renderExplanation(q.explanation)}</div>`;
    html += `</div>`;
  }

  html += `</div>`;
  questionArea.innerHTML = html;

  // Attach click handlers to option buttons
  questionArea.querySelectorAll(".option-btn").forEach((btn) => {
    btn.addEventListener("click", () => onOptionClick(Number(btn.dataset.idx)));
  });
}

// ─── Event handlers ──────────────────────────────────────────
function onOptionClick(idx) {
  const s = getState(currentIndex);
  if (s.checked) return;

  // Single-choice: toggle or switch
  s.selected = s.selected === idx ? null : idx;
  render();
}

btnCheck.addEventListener("click", () => {
  const s = getState(currentIndex);
  if (s.selected === null || s.checked) return;
  s.checked = true;
  btnCheck.disabled = true;
  render();
});

btnPrev.addEventListener("click", () => {
  if (currentIndex > 0) {
    currentIndex--;
    render();
  }
});

btnNext.addEventListener("click", () => {
  if (currentIndex < questions.length - 1) {
    currentIndex++;
    render();
  }
});

// Keyboard shortcuts
document.addEventListener("keydown", (e) => {
  if (e.key === "ArrowLeft" && currentIndex > 0) {
    currentIndex--;
    render();
  } else if (e.key === "ArrowRight" && currentIndex < questions.length - 1) {
    currentIndex++;
    render();
  } else if (e.key === "Enter") {
    btnCheck.click();
  }
});

// ─── Util ────────────────────────────────────────────────────
function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

/**
 * Render explanation text with light-markdown support.
 * Supports: **bold**, `code`, \n\n for paragraphs, \n for line breaks.
 * Runs escapeHtml first for XSS safety, then applies formatting.
 */
function renderExplanation(str) {
  let html = escapeHtml(str);
  // \n\n → paragraph break
  html = html
    .split("\n\n")
    .map((p) => `<p>${p}</p>`)
    .join("");
  // \n → <br> inside paragraphs
  html = html.replace(/\n/g, "<br>");
  // **bold** → <strong>
  html = html.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
  // `code` → <code>
  html = html.replace(/`(.+?)`/g, "<code>$1</code>");
  // ≈ analogy marker → styled span
  html = html.replace(/≈\s/g, '<span class="analogy">≈ </span>');
  return html;
}

// Go!
init();
