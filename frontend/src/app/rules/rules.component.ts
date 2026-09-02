import { Component } from '@angular/core';

interface RuleItem {
  text: string;
}

const RULES: RuleItem[] = [
  { text: 'Video should be between 5 to 20 seconds.' },
  { text: 'Only one player should be in the video.' },
  { text: 'The player and their action should be clearly visible.' },
  { text: 'The video should contain only actions like shot, dribbling and bowling — no warm-up, standing around, etc.' },
  { text: 'The video should not contain any text or watermark.' },
];

const DEMO_VIDEO_SRC = '/demovideo.mp4';

@Component({
  selector: 'app-rules',
  imports: [],
  templateUrl: './rules.component.html',
  styleUrl: './rules.component.scss'
})
export class RulesComponent {
  readonly rules = RULES;
  readonly demoVideoSrc = DEMO_VIDEO_SRC;
}