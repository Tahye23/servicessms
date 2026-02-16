import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IReponse } from '../reponse.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../reponse.test-samples';

import { ReponseService } from './reponse.service';

const requireRestSample: IReponse = {
  ...sampleWithRequiredData,
};

describe('Reponse Service', () => {
  let service: ReponseService;
  let httpMock: HttpTestingController;
  let expectedResult: IReponse | IReponse[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(ReponseService);
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

    it('should create a Reponse', () => {
      const reponse = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(reponse).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Reponse', () => {
      const reponse = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(reponse).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Reponse', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Reponse', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Reponse', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addReponseToCollectionIfMissing', () => {
      it('should add a Reponse to an empty array', () => {
        const reponse: IReponse = sampleWithRequiredData;
        expectedResult = service.addReponseToCollectionIfMissing([], reponse);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(reponse);
      });

      it('should not add a Reponse to an array that contains it', () => {
        const reponse: IReponse = sampleWithRequiredData;
        const reponseCollection: IReponse[] = [
          {
            ...reponse,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addReponseToCollectionIfMissing(reponseCollection, reponse);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Reponse to an array that doesn't contain it", () => {
        const reponse: IReponse = sampleWithRequiredData;
        const reponseCollection: IReponse[] = [sampleWithPartialData];
        expectedResult = service.addReponseToCollectionIfMissing(reponseCollection, reponse);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(reponse);
      });

      it('should add only unique Reponse to an array', () => {
        const reponseArray: IReponse[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const reponseCollection: IReponse[] = [sampleWithRequiredData];
        expectedResult = service.addReponseToCollectionIfMissing(reponseCollection, ...reponseArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const reponse: IReponse = sampleWithRequiredData;
        const reponse2: IReponse = sampleWithPartialData;
        expectedResult = service.addReponseToCollectionIfMissing([], reponse, reponse2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(reponse);
        expect(expectedResult).toContain(reponse2);
      });

      it('should accept null and undefined values', () => {
        const reponse: IReponse = sampleWithRequiredData;
        expectedResult = service.addReponseToCollectionIfMissing([], null, reponse, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(reponse);
      });

      it('should return initial array if no Reponse is added', () => {
        const reponseCollection: IReponse[] = [sampleWithRequiredData];
        expectedResult = service.addReponseToCollectionIfMissing(reponseCollection, undefined, null);
        expect(expectedResult).toEqual(reponseCollection);
      });
    });

    describe('compareReponse', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareReponse(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareReponse(entity1, entity2);
        const compareResult2 = service.compareReponse(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareReponse(entity1, entity2);
        const compareResult2 = service.compareReponse(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareReponse(entity1, entity2);
        const compareResult2 = service.compareReponse(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
