import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IEntitedetest } from '../entitedetest.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../entitedetest.test-samples';

import { EntitedetestService, RestEntitedetest } from './entitedetest.service';

const requireRestSample: RestEntitedetest = {
  ...sampleWithRequiredData,
  champdate: sampleWithRequiredData.champdate?.toJSON(),
};

describe('Entitedetest Service', () => {
  let service: EntitedetestService;
  let httpMock: HttpTestingController;
  let expectedResult: IEntitedetest | IEntitedetest[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(EntitedetestService);
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

    it('should create a Entitedetest', () => {
      const entitedetest = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(entitedetest).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Entitedetest', () => {
      const entitedetest = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(entitedetest).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Entitedetest', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Entitedetest', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Entitedetest', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addEntitedetestToCollectionIfMissing', () => {
      it('should add a Entitedetest to an empty array', () => {
        const entitedetest: IEntitedetest = sampleWithRequiredData;
        expectedResult = service.addEntitedetestToCollectionIfMissing([], entitedetest);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(entitedetest);
      });

      it('should not add a Entitedetest to an array that contains it', () => {
        const entitedetest: IEntitedetest = sampleWithRequiredData;
        const entitedetestCollection: IEntitedetest[] = [
          {
            ...entitedetest,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addEntitedetestToCollectionIfMissing(entitedetestCollection, entitedetest);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Entitedetest to an array that doesn't contain it", () => {
        const entitedetest: IEntitedetest = sampleWithRequiredData;
        const entitedetestCollection: IEntitedetest[] = [sampleWithPartialData];
        expectedResult = service.addEntitedetestToCollectionIfMissing(entitedetestCollection, entitedetest);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(entitedetest);
      });

      it('should add only unique Entitedetest to an array', () => {
        const entitedetestArray: IEntitedetest[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const entitedetestCollection: IEntitedetest[] = [sampleWithRequiredData];
        expectedResult = service.addEntitedetestToCollectionIfMissing(entitedetestCollection, ...entitedetestArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const entitedetest: IEntitedetest = sampleWithRequiredData;
        const entitedetest2: IEntitedetest = sampleWithPartialData;
        expectedResult = service.addEntitedetestToCollectionIfMissing([], entitedetest, entitedetest2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(entitedetest);
        expect(expectedResult).toContain(entitedetest2);
      });

      it('should accept null and undefined values', () => {
        const entitedetest: IEntitedetest = sampleWithRequiredData;
        expectedResult = service.addEntitedetestToCollectionIfMissing([], null, entitedetest, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(entitedetest);
      });

      it('should return initial array if no Entitedetest is added', () => {
        const entitedetestCollection: IEntitedetest[] = [sampleWithRequiredData];
        expectedResult = service.addEntitedetestToCollectionIfMissing(entitedetestCollection, undefined, null);
        expect(expectedResult).toEqual(entitedetestCollection);
      });
    });

    describe('compareEntitedetest', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareEntitedetest(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareEntitedetest(entity1, entity2);
        const compareResult2 = service.compareEntitedetest(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareEntitedetest(entity1, entity2);
        const compareResult2 = service.compareEntitedetest(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareEntitedetest(entity1, entity2);
        const compareResult2 = service.compareEntitedetest(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
