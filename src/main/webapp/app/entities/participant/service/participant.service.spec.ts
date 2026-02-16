import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IParticipant } from '../participant.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../participant.test-samples';

import { ParticipantService } from './participant.service';

const requireRestSample: IParticipant = {
  ...sampleWithRequiredData,
};

describe('Participant Service', () => {
  let service: ParticipantService;
  let httpMock: HttpTestingController;
  let expectedResult: IParticipant | IParticipant[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(ParticipantService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a Participant', () => {
      const participant = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(participant).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Participant', () => {
      const participant = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(participant).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Participant', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Participant', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Participant', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addParticipantToCollectionIfMissing', () => {
      it('should add a Participant to an empty array', () => {
        const participant: IParticipant = sampleWithRequiredData;
        expectedResult = service.addParticipantToCollectionIfMissing([], participant);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(participant);
      });

      it('should not add a Participant to an array that contains it', () => {
        const participant: IParticipant = sampleWithRequiredData;
        const participantCollection: IParticipant[] = [
          {
            ...participant,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addParticipantToCollectionIfMissing(participantCollection, participant);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Participant to an array that doesn't contain it", () => {
        const participant: IParticipant = sampleWithRequiredData;
        const participantCollection: IParticipant[] = [sampleWithPartialData];
        expectedResult = service.addParticipantToCollectionIfMissing(participantCollection, participant);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(participant);
      });

      it('should add only unique Participant to an array', () => {
        const participantArray: IParticipant[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const participantCollection: IParticipant[] = [sampleWithRequiredData];
        expectedResult = service.addParticipantToCollectionIfMissing(participantCollection, ...participantArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const participant: IParticipant = sampleWithRequiredData;
        const participant2: IParticipant = sampleWithPartialData;
        expectedResult = service.addParticipantToCollectionIfMissing([], participant, participant2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(participant);
        expect(expectedResult).toContain(participant2);
      });

      it('should accept null and undefined values', () => {
        const participant: IParticipant = sampleWithRequiredData;
        expectedResult = service.addParticipantToCollectionIfMissing([], null, participant, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(participant);
      });

      it('should return initial array if no Participant is added', () => {
        const participantCollection: IParticipant[] = [sampleWithRequiredData];
        expectedResult = service.addParticipantToCollectionIfMissing(participantCollection, undefined, null);
        expect(expectedResult).toEqual(participantCollection);
      });
    });

    describe('compareParticipant', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareParticipant(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareParticipant(entity1, entity2);
        const compareResult2 = service.compareParticipant(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareParticipant(entity1, entity2);
        const compareResult2 = service.compareParticipant(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareParticipant(entity1, entity2);
        const compareResult2 = service.compareParticipant(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
