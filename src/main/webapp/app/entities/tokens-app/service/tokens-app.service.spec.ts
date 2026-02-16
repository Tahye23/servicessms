import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ITokensApp } from '../tokens-app.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../tokens-app.test-samples';

import { TokensAppService, RestTokensApp } from './tokens-app.service';

const requireRestSample: RestTokensApp = {
  ...sampleWithRequiredData,
  dateExpiration: sampleWithRequiredData.dateExpiration?.toJSON(),
};

describe('TokensApp Service', () => {
  let service: TokensAppService;
  let httpMock: HttpTestingController;
  let expectedResult: ITokensApp | ITokensApp[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(TokensAppService);
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

    it('should create a TokensApp', () => {
      const tokensApp = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(tokensApp).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a TokensApp', () => {
      const tokensApp = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(tokensApp).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a TokensApp', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of TokensApp', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a TokensApp', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addTokensAppToCollectionIfMissing', () => {
      it('should add a TokensApp to an empty array', () => {
        const tokensApp: ITokensApp = sampleWithRequiredData;
        expectedResult = service.addTokensAppToCollectionIfMissing([], tokensApp);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(tokensApp);
      });

      it('should not add a TokensApp to an array that contains it', () => {
        const tokensApp: ITokensApp = sampleWithRequiredData;
        const tokensAppCollection: ITokensApp[] = [
          {
            ...tokensApp,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addTokensAppToCollectionIfMissing(tokensAppCollection, tokensApp);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a TokensApp to an array that doesn't contain it", () => {
        const tokensApp: ITokensApp = sampleWithRequiredData;
        const tokensAppCollection: ITokensApp[] = [sampleWithPartialData];
        expectedResult = service.addTokensAppToCollectionIfMissing(tokensAppCollection, tokensApp);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(tokensApp);
      });

      it('should add only unique TokensApp to an array', () => {
        const tokensAppArray: ITokensApp[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const tokensAppCollection: ITokensApp[] = [sampleWithRequiredData];
        expectedResult = service.addTokensAppToCollectionIfMissing(tokensAppCollection, ...tokensAppArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const tokensApp: ITokensApp = sampleWithRequiredData;
        const tokensApp2: ITokensApp = sampleWithPartialData;
        expectedResult = service.addTokensAppToCollectionIfMissing([], tokensApp, tokensApp2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(tokensApp);
        expect(expectedResult).toContain(tokensApp2);
      });

      it('should accept null and undefined values', () => {
        const tokensApp: ITokensApp = sampleWithRequiredData;
        expectedResult = service.addTokensAppToCollectionIfMissing([], null, tokensApp, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(tokensApp);
      });

      it('should return initial array if no TokensApp is added', () => {
        const tokensAppCollection: ITokensApp[] = [sampleWithRequiredData];
        expectedResult = service.addTokensAppToCollectionIfMissing(tokensAppCollection, undefined, null);
        expect(expectedResult).toEqual(tokensAppCollection);
      });
    });

    describe('compareTokensApp', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareTokensApp(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareTokensApp(entity1, entity2);
        const compareResult2 = service.compareTokensApp(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareTokensApp(entity1, entity2);
        const compareResult2 = service.compareTokensApp(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareTokensApp(entity1, entity2);
        const compareResult2 = service.compareTokensApp(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
