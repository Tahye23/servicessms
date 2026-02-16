import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IOTPStorage } from '../otp-storage.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../otp-storage.test-samples';

import { OTPStorageService, RestOTPStorage } from './otp-storage.service';

const requireRestSample: RestOTPStorage = {
  ...sampleWithRequiredData,
  otsdateexpir: sampleWithRequiredData.otsdateexpir?.toJSON(),
};

describe('OTPStorage Service', () => {
  let service: OTPStorageService;
  let httpMock: HttpTestingController;
  let expectedResult: IOTPStorage | IOTPStorage[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(OTPStorageService);
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

    it('should create a OTPStorage', () => {
      const oTPStorage = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(oTPStorage).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a OTPStorage', () => {
      const oTPStorage = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(oTPStorage).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a OTPStorage', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of OTPStorage', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a OTPStorage', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addOTPStorageToCollectionIfMissing', () => {
      it('should add a OTPStorage to an empty array', () => {
        const oTPStorage: IOTPStorage = sampleWithRequiredData;
        expectedResult = service.addOTPStorageToCollectionIfMissing([], oTPStorage);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(oTPStorage);
      });

      it('should not add a OTPStorage to an array that contains it', () => {
        const oTPStorage: IOTPStorage = sampleWithRequiredData;
        const oTPStorageCollection: IOTPStorage[] = [
          {
            ...oTPStorage,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addOTPStorageToCollectionIfMissing(oTPStorageCollection, oTPStorage);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a OTPStorage to an array that doesn't contain it", () => {
        const oTPStorage: IOTPStorage = sampleWithRequiredData;
        const oTPStorageCollection: IOTPStorage[] = [sampleWithPartialData];
        expectedResult = service.addOTPStorageToCollectionIfMissing(oTPStorageCollection, oTPStorage);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(oTPStorage);
      });

      it('should add only unique OTPStorage to an array', () => {
        const oTPStorageArray: IOTPStorage[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const oTPStorageCollection: IOTPStorage[] = [sampleWithRequiredData];
        expectedResult = service.addOTPStorageToCollectionIfMissing(oTPStorageCollection, ...oTPStorageArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const oTPStorage: IOTPStorage = sampleWithRequiredData;
        const oTPStorage2: IOTPStorage = sampleWithPartialData;
        expectedResult = service.addOTPStorageToCollectionIfMissing([], oTPStorage, oTPStorage2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(oTPStorage);
        expect(expectedResult).toContain(oTPStorage2);
      });

      it('should accept null and undefined values', () => {
        const oTPStorage: IOTPStorage = sampleWithRequiredData;
        expectedResult = service.addOTPStorageToCollectionIfMissing([], null, oTPStorage, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(oTPStorage);
      });

      it('should return initial array if no OTPStorage is added', () => {
        const oTPStorageCollection: IOTPStorage[] = [sampleWithRequiredData];
        expectedResult = service.addOTPStorageToCollectionIfMissing(oTPStorageCollection, undefined, null);
        expect(expectedResult).toEqual(oTPStorageCollection);
      });
    });

    describe('compareOTPStorage', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareOTPStorage(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareOTPStorage(entity1, entity2);
        const compareResult2 = service.compareOTPStorage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareOTPStorage(entity1, entity2);
        const compareResult2 = service.compareOTPStorage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareOTPStorage(entity1, entity2);
        const compareResult2 = service.compareOTPStorage(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
