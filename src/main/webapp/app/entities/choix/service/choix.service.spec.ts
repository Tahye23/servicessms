import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IChoix } from '../choix.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../choix.test-samples';

import { ChoixService } from './choix.service';

const requireRestSample: IChoix = {
  ...sampleWithRequiredData,
};

describe('Choix Service', () => {
  let service: ChoixService;
  let httpMock: HttpTestingController;
  let expectedResult: IChoix | IChoix[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(ChoixService);
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

    it('should create a Choix', () => {
      const choix = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(choix).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Choix', () => {
      const choix = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(choix).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Choix', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Choix', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Choix', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addChoixToCollectionIfMissing', () => {
      it('should add a Choix to an empty array', () => {
        const choix: IChoix = sampleWithRequiredData;
        expectedResult = service.addChoixToCollectionIfMissing([], choix);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(choix);
      });

      it('should not add a Choix to an array that contains it', () => {
        const choix: IChoix = sampleWithRequiredData;
        const choixCollection: IChoix[] = [
          {
            ...choix,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addChoixToCollectionIfMissing(choixCollection, choix);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Choix to an array that doesn't contain it", () => {
        const choix: IChoix = sampleWithRequiredData;
        const choixCollection: IChoix[] = [sampleWithPartialData];
        expectedResult = service.addChoixToCollectionIfMissing(choixCollection, choix);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(choix);
      });

      it('should add only unique Choix to an array', () => {
        const choixArray: IChoix[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const choixCollection: IChoix[] = [sampleWithRequiredData];
        expectedResult = service.addChoixToCollectionIfMissing(choixCollection, ...choixArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const choix: IChoix = sampleWithRequiredData;
        const choix2: IChoix = sampleWithPartialData;
        expectedResult = service.addChoixToCollectionIfMissing([], choix, choix2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(choix);
        expect(expectedResult).toContain(choix2);
      });

      it('should accept null and undefined values', () => {
        const choix: IChoix = sampleWithRequiredData;
        expectedResult = service.addChoixToCollectionIfMissing([], null, choix, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(choix);
      });

      it('should return initial array if no Choix is added', () => {
        const choixCollection: IChoix[] = [sampleWithRequiredData];
        expectedResult = service.addChoixToCollectionIfMissing(choixCollection, undefined, null);
        expect(expectedResult).toEqual(choixCollection);
      });
    });

    describe('compareChoix', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareChoix(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareChoix(entity1, entity2);
        const compareResult2 = service.compareChoix(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareChoix(entity1, entity2);
        const compareResult2 = service.compareChoix(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareChoix(entity1, entity2);
        const compareResult2 = service.compareChoix(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
