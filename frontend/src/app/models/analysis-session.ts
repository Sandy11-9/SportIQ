import { Score } from './score';

export interface AnalysisSession {
  id: number;
  sportId: number;
  sportName: string;
  skillId: number;
  skillName: string;
  analyzedAt: string;
  scores: Score[];
  goodPoints: string[];
  badPoints: string[];
  improvements: string[];
}
