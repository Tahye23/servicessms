import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IAbonnement } from '../abonnement.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../abonnement.test-samples';

import { AbonnementService } from './abonnement.service';

const requireRestSample: IAbonnement = {
  ...sampleWithRequiredData,
};

describe('Abonnement Service', () => {
  let service: AbonnementService;
  let httpMock: HttpTestingController;
  let expectedResult: IAbonnement | IAbonnement[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(AbonnementService);
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

    it('should create a Abonnement', () => {
      const abonnement = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(abonnement).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Abonnement', () => {
      const abonnement = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(abonnement).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Abonnement', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Abonnement', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Abonnement', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addAbonnementToCollectionIfMissing', () => {
      it('should add a Abonnement to an empty array', () => {
        const abonnement: IAbonnement = sampleWithRequiredData;
        expectedResult = service.addAbonnementToCollectionIfMissing([], abonnement);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(abonnement);
      });

      it('should not add a Abonnement to an array that contains it', () => {
        const abonnement: IAbonnement = sampleWithRequiredData;
        const abonnementCollection: IAbonnement[] = [
          {
            ...abonnement,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addAbonnementToCollectionIfMissing(abonnementCollection, abonnement);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Abonnement to an array that doesn't contain it", () => {
        const abonnement: IAbonnement = sampleWithRequiredData;
        const abonnementCollection: IAbonnement[] = [sampleWithPartialData];
        expectedResult = service.addAbonnementToCollectionIfMissing(abonnementCollection, abonnement);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(abonnement);
      });

      it('should add only unique Abonnement to an array', () => {
        const abonnementArray: IAbonnement[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const abonnementCollection: IAbonnement[] = [sampleWithRequiredData];
        expectedResult = service.addAbonnementToCollectionIfMissing(abonnementCollection, ...abonnementArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const abonnement: IAbonnement = sampleWithRequiredData;
        const abonnement2: IAbonnement = sampleWithPartialData;
        expectedResult = service.addAbonnementToCollectionIfMissing([], abonnement, abonnement2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(abonnement);
        expect(expectedResult).toContain(abonnement2);
      });

      it('should accept null and undefined values', () => {
        const abonnement: IAbonnement = sampleWithRequiredData;
        expectedResult = service.addAbonnementToCollectionIfMissing([], null, abonnement, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(abonnement);
      });

      it('should return initial array if no Abonnement is added', () => {
        const abonnementCollection: IAbonnement[] = [sampleWithRequiredData];
        expectedResult = service.addAbonnementToCollectionIfMissing(abonnementCollection, undefined, null);
        expect(expectedResult).toEqual(abonnementCollection);
      });
    });

    describe('compareAbonnement', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareAbonnement(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareAbonnement(entity1, entity2);
        const compareResult2 = service.compareAbonnement(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareAbonnement(entity1, entity2);
        const compareResult2 = service.compareAbonnement(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareAbonnement(entity1, entity2);
        const compareResult2 = service.compareAbonnement(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
