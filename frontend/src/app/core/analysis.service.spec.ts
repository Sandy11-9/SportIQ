import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalysisService } from './analysis.service';
import { AnalysisSession } from '../models/analysis-session';

const SESSION: AnalysisSession = {
  id: 9,
  sportId: 1,
  sportName: 'Cricket',
  skillId: 1,
  skillName: 'Bowling',
  analyzedAt: '2026-08-11T00:00:00Z',
  scores: [{ category: 'Action', score: 82 }],
  goodPoints: ['clean strike'],
  badPoints: [],
  improvements: ['repeat drill'],
};

describe('AnalysisService', () => {
  let service: AnalysisService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AnalysisService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should upload a video as multipart form data', () => {
    const file = new File(['x'], 'clip.mp4', { type: 'video/mp4' });

    service.analyze(file, 1, 1).subscribe((result) => expect(result).toEqual(SESSION));

    const req = httpMock.expectOne('/api/analyses?sportId=1&skillId=1');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    expect(req.request.body.get('file')).toEqual(file);
    req.flush(SESSION);
  });

  it('should list sessions with filters', () => {
    const page = { content: [SESSION], totalElements: 1, totalPages: 1, number: 0, size: 20 };

    service.list(0, 20, 1, 1).subscribe((result) => expect(result).toEqual(page));

    const req = httpMock.expectOne('/api/analyses?page=0&size=20&sportId=1&skillId=1');
    expect(req.request.method).toBe('GET');
    req.flush(page);
  });

  it('should list sessions without filters', () => {
    service.list(0, 20).subscribe();

    const req = httpMock.expectOne('/api/analyses?page=0&size=20');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('should fetch a single session', () => {
    service.get(9).subscribe((result) => expect(result).toEqual(SESSION));

    const req = httpMock.expectOne('/api/analyses/9');
    expect(req.request.method).toBe('GET');
    req.flush(SESSION);
  });
});
