import {
  afterNextRender,
  Component,
  effect,
  ElementRef,
  inject,
  input,
  model,
  OnDestroy,
  viewChild,
} from '@angular/core';
import {Compartment, EditorState, Extension} from '@codemirror/state';
import {EditorView, keymap, placeholder as cmPlaceholder} from '@codemirror/view';
import {defaultKeymap, history, historyKeymap} from '@codemirror/commands';
import {HighlightStyle, StreamLanguage, syntaxHighlighting} from '@codemirror/language';
import {json} from '@codemirror/lang-json';
import {tags} from '@lezer/highlight';
import {ThemeService} from '../../../core/services/theme.service';

// ── HL7v2 Language Definition ──

const hl7v2Language = StreamLanguage.define({
  startState: () => ({atLineStart: true}),
  token(stream, state) {
    if (stream.sol()) {
      state.atLineStart = true;
    }

    if (state.atLineStart) {
      state.atLineStart = false;
      if (stream.match(/^[A-Z][A-Z0-9]{2}/)) {
        return 'keyword';
      }
    }

    const ch = stream.peek();
    if (ch === '|') {
      stream.next();
      return 'punctuation';
    }
    if (ch === '^') {
      stream.next();
      return 'operator';
    }
    if (ch === '~') {
      stream.next();
      return 'meta';
    }
    if (ch === '&') {
      stream.next();
      return 'atom';
    }

    if (ch === '\\') {
      stream.next();
      if (stream.match(/^[FSTRE]\\/) || stream.match(/^X[0-9A-Fa-f]*\\/)) {
        return 'escape';
      }
      return null;
    }

    stream.match(/^[^|^~&\\]+/);
    return null;
  },
});

// ── Syntax Highlighting Styles ──

function buildHighlightStyle(isDark: boolean): HighlightStyle {
  return HighlightStyle.define([
    {tag: tags.keyword, color: 'var(--p-primary-color)', fontWeight: 'bold'},
    {tag: tags.punctuation, color: isDark ? 'var(--p-orange-400)' : 'var(--p-orange-600)', fontWeight: 'bold'},
    {tag: tags.operator, color: isDark ? 'var(--p-cyan-400)' : 'var(--p-cyan-600)'},
    {tag: tags.meta, color: isDark ? 'var(--p-purple-400)' : 'var(--p-purple-600)'},
    {tag: tags.atom, color: isDark ? 'var(--p-teal-400)' : 'var(--p-teal-600)'},
    {tag: tags.escape, color: isDark ? 'var(--p-red-400)' : 'var(--p-red-600)', fontStyle: 'italic'},
    // JSON tokens
    {tag: tags.propertyName, color: isDark ? 'var(--p-cyan-400)' : 'var(--p-cyan-700)'},
    {tag: tags.string, color: isDark ? 'var(--p-green-400)' : 'var(--p-green-700)'},
    {tag: tags.number, color: isDark ? 'var(--p-orange-400)' : 'var(--p-orange-700)'},
    {tag: tags.bool, color: isDark ? 'var(--p-purple-400)' : 'var(--p-purple-600)', fontWeight: 'bold'},
    {tag: tags.null, color: isDark ? 'var(--p-red-400)' : 'var(--p-red-600)', fontStyle: 'italic'},
  ]);
}

// ── Editor Theme ──

function buildEditorTheme(isDark: boolean, height?: string): Extension {
  return [
    EditorView.theme({
      '&': {
        backgroundColor: 'var(--p-content-background)',
        color: 'var(--p-text-color)',
        border: '1px solid var(--p-content-border-color)',
        borderRadius: '6px',
        ...(height && height !== 'auto' ? {height} : {}),
      },
      '&.cm-focused': {
        outline: '1px solid var(--p-primary-color)',
        borderColor: 'var(--p-primary-color)',
      },
      '.cm-cursor': {
        borderLeftColor: 'var(--p-text-color)',
      },
      '.cm-selectionBackground': {
        backgroundColor: isDark ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.08)',
      },
      '&.cm-focused .cm-selectionBackground': {
        backgroundColor: isDark ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 120, 0, 0.12)',
      },
      '.cm-content': {
        caretColor: 'var(--p-text-color)',
        padding: '0.5rem 0',
        fontFamily: "'Cascadia Code', 'Fira Code', 'Consolas', monospace",
        fontSize: '0.8rem',
      },
      '.cm-line': {
        padding: '0 0.75rem',
      },
      '.cm-placeholder': {
        color: 'var(--p-text-muted-color)',
      },
      '.cm-scroller': {
        overflow: 'auto',
      },
    }, {dark: isDark}),
    syntaxHighlighting(buildHighlightStyle(isDark)),
  ];
}

// ── Component ──

@Component({
  selector: 'app-input-editor',
  standalone: true,
  template: `<div class="editor-container" #editorHost></div>`,
  styles: `:host { display: block; }`,
})
export class InputEditorComponent implements OnDestroy {
  private themeService = inject(ThemeService);

  content = model('');
  readonly = input(false);
  placeholder = input('');
  height = input('auto');
  language = input<'hl7v2' | 'json' | 'none'>('hl7v2');

  private editorHost = viewChild.required<ElementRef>('editorHost');

  private view: EditorView | null = null;
  private themeCompartment = new Compartment();
  private readonlyCompartment = new Compartment();
  private languageCompartment = new Compartment();
  private suppressSync = false;

  constructor() {
    afterNextRender(() => this.initEditor());

    effect(() => {
      const isDark = this.themeService.darkMode();
      this.view?.dispatch({
        effects: this.themeCompartment.reconfigure(buildEditorTheme(isDark, this.height())),
      });
    });

    effect(() => {
      const ro = this.readonly();
      this.view?.dispatch({
        effects: this.readonlyCompartment.reconfigure(EditorView.editable.of(!ro)),
      });
    });

    effect(() => {
      const value = this.content();
      if (this.suppressSync) return;
      if (this.view && this.view.state.doc.toString() !== value) {
        this.view.dispatch({
          changes: {from: 0, to: this.view.state.doc.length, insert: value},
        });
      }
    });
  }

  private getLanguageExtension(): Extension {
    switch (this.language()) {
      case 'json':
        return json();
      case 'hl7v2':
        return hl7v2Language;
      default:
        return [];
    }
  }

  private initEditor(): void {
    const host = this.editorHost().nativeElement as HTMLElement;

    const state = EditorState.create({
      doc: this.content(),
      extensions: [
        history(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        this.themeCompartment.of(buildEditorTheme(this.themeService.darkMode(), this.height())),
        this.readonlyCompartment.of(EditorView.editable.of(!this.readonly())),
        this.languageCompartment.of(this.getLanguageExtension()),
        cmPlaceholder(this.placeholder()),
        EditorView.lineWrapping,
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            const doc = update.state.doc.toString();
            if (this.content() !== doc) {
              this.suppressSync = true;
              this.content.set(doc);
              this.suppressSync = false;
            }
          }
        }),
      ],
    });

    this.view = new EditorView({state, parent: host});
  }

  ngOnDestroy(): void {
    this.view?.destroy();
  }
}
