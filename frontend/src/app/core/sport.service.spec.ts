import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SportService } from './sport.service';

describe('SportService', () => {
  let service: SportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(SportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list sports', () => {
    const sports = [{ id: 1, name: 'Cricket' }];

    service.listSports().subscribe((result) => expect(result).toEqual(sports));

    const req = httpMock.expectOne('/api/sports');
    expect(req.request.method).toBe('GET');
    req.flush(sports);
  });

  it('should list skills for a sport', () => {
    const skills = [{ id: 1, sportId: 1, name: 'Bowling' }];

    service.listSkills(1).subscribe((result) => expect(result).toEqual(skills));

    const req = httpMock.expectOne('/api/sports/1/skills');
    expect(req.request.method).toBe('GET');
    req.flush(skills);
  });
});
