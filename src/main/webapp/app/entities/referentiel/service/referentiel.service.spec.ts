import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IReferentiel } from '../referentiel.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../referentiel.test-samples';

import { ReferentielService } from './referentiel.service';

const requireRestSample: IReferentiel = {
  ...sampleWithRequiredData,
};

describe('Referentiel Service', () => {
  let service: ReferentielService;
  let httpMock: HttpTestingController;
  let expectedResult: IReferentiel | IReferentiel[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(ReferentielService);
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

    it('should create a Referentiel', () => {
      const referentiel = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(referentiel).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Referentiel', () => {
      const referentiel = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(referentiel).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Referentiel', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Referentiel', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Referentiel', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addReferentielToCollectionIfMissing', () => {
      it('should add a Referentiel to an empty array', () => {
        const referentiel: IReferentiel = sampleWithRequiredData;
        expectedResult = service.addReferentielToCollectionIfMissing([], referentiel);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(referentiel);
      });

      it('should not add a Referentiel to an array that contains it', () => {
        const referentiel: IReferentiel = sampleWithRequiredData;
        const referentielCollection: IReferentiel[] = [
          {
            ...referentiel,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addReferentielToCollectionIfMissing(referentielCollection, referentiel);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Referentiel to an array that doesn't contain it", () => {
        const referentiel: IReferentiel = sampleWithRequiredData;
        const referentielCollection: IReferentiel[] = [sampleWithPartialData];
        expectedResult = service.addReferentielToCollectionIfMissing(referentielCollection, referentiel);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(referentiel);
      });

      it('should add only unique Referentiel to an array', () => {
        const referentielArray: IReferentiel[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const referentielCollection: IReferentiel[] = [sampleWithRequiredData];
        expectedResult = service.addReferentielToCollectionIfMissing(referentielCollection, ...referentielArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const referentiel: IReferentiel = sampleWithRequiredData;
        const referentiel2: IReferentiel = sampleWithPartialData;
        expectedResult = service.addReferentielToCollectionIfMissing([], referentiel, referentiel2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(referentiel);
        expect(expectedResult).toContain(referentiel2);
      });

      it('should accept null and undefined values', () => {
        const referentiel: IReferentiel = sampleWithRequiredData;
        expectedResult = service.addReferentielToCollectionIfMissing([], null, referentiel, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(referentiel);
      });

      it('should return initial array if no Referentiel is added', () => {
        const referentielCollection: IReferentiel[] = [sampleWithRequiredData];
        expectedResult = service.addReferentielToCollectionIfMissing(referentielCollection, undefined, null);
        expect(expectedResult).toEqual(referentielCollection);
      });
    });

    describe('compareReferentiel', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareReferentiel(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareReferentiel(entity1, entity2);
        const compareResult2 = service.compareReferentiel(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareReferentiel(entity1, entity2);
        const compareResult2 = service.compareReferentiel(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareReferentiel(entity1, entity2);
        const compareResult2 = service.compareReferentiel(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
