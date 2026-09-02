import { Component, DestroyRef, effect, inject, signal, viewChild } from '@angular/core';
import { ElementRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChatMessage } from '../models/chat';
import { ChatService } from '../core/chat.service';

const MAX_MESSAGE_LENGTH = 2000;
const UNAVAILABLE_REPLY = 'SportIQ Assistant is temporarily unavailable. Please try again.';

const INITIAL_MESSAGE: ChatMessage = {
  role: 'assistant',
  content:
    "Hi! I'm SportIQ Assistant. Ask me anything about sports, games, training, players, teams, rules, or sports analysis.",
};

@Component({
  selector: 'app-chatbot',
  imports: [],
  templateUrl: './chatbot.component.html',
  styleUrl: './chatbot.component.scss'
})
export class ChatbotComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly messagesEl = viewChild<ElementRef<HTMLElement>>('messagesEl');

  readonly open = signal(false);
  readonly messages = signal<ChatMessage[]>([{ ...INITIAL_MESSAGE }]);
  readonly draft = signal('');
  readonly waiting = signal(false);
  readonly error = signal<string | null>(null);

  get maxLength(): number {
    return MAX_MESSAGE_LENGTH;
  }

  constructor(private readonly chatService: ChatService) {
    effect(() => {
      this.open();
      this.waiting();
      this.messages();
      requestAnimationFrame(() => this.scrollToBottom());
    });
  }

  toggle(): void {
    this.open.update((open) => !open);
  }

  minimize(): void {
    this.open.set(false);
  }

  onInput(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.draft.set(textarea.value);
    this.error.set(null);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  send(): void {
    if (this.waiting()) {
      return;
    }
    const message = this.draft().trim();
    if (!message) {
      this.error.set('Please enter a sports question.');
      return;
    }
    this.error.set(null);
    this.draft.set('');
    this.messages.update((all) => [...all, { role: 'user', content: message }]);
    this.waiting.set(true);
    this.chatService.send(message)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.waiting.set(false);
          this.messages.update((all) => [...all, { role: 'assistant', content: response.reply }]);
        },
        error: () => {
          this.waiting.set(false);
          this.messages.update((all) => [...all, { role: 'assistant', content: UNAVAILABLE_REPLY }]);
        },
      });
  }

  private scrollToBottom(): void {
    const el = this.messagesEl()?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}