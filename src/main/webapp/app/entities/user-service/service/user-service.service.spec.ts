import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IUserService } from '../user-service.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../user-service.test-samples';

import { UserServiceService } from './user-service.service';

const requireRestSample: IUserService = {
  ...sampleWithRequiredData,
};

describe('UserService Service', () => {
  let service: UserServiceService;
  let httpMock: HttpTestingController;
  let expectedResult: IUserService | IUserService[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(UserServiceService);
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

    it('should create a UserService', () => {
      const userService = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(userService).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a UserService', () => {
      const userService = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(userService).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a UserService', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of UserService', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a UserService', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addUserServiceToCollectionIfMissing', () => {
      it('should add a UserService to an empty array', () => {
        const userService: IUserService = sampleWithRequiredData;
        expectedResult = service.addUserServiceToCollectionIfMissing([], userService);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(userService);
      });

      it('should not add a UserService to an array that contains it', () => {
        const userService: IUserService = sampleWithRequiredData;
        const userServiceCollection: IUserService[] = [
          {
            ...userService,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addUserServiceToCollectionIfMissing(userServiceCollection, userService);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a UserService to an array that doesn't contain it", () => {
        const userService: IUserService = sampleWithRequiredData;
        const userServiceCollection: IUserService[] = [sampleWithPartialData];
        expectedResult = service.addUserServiceToCollectionIfMissing(userServiceCollection, userService);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(userService);
      });

      it('should add only unique UserService to an array', () => {
        const userServiceArray: IUserService[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const userServiceCollection: IUserService[] = [sampleWithRequiredData];
        expectedResult = service.addUserServiceToCollectionIfMissing(userServiceCollection, ...userServiceArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const userService: IUserService = sampleWithRequiredData;
        const userService2: IUserService = sampleWithPartialData;
        expectedResult = service.addUserServiceToCollectionIfMissing([], userService, userService2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(userService);
        expect(expectedResult).toContain(userService2);
      });

      it('should accept null and undefined values', () => {
        const userService: IUserService = sampleWithRequiredData;
        expectedResult = service.addUserServiceToCollectionIfMissing([], null, userService, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(userService);
      });

      it('should return initial array if no UserService is added', () => {
        const userServiceCollection: IUserService[] = [sampleWithRequiredData];
        expectedResult = service.addUserServiceToCollectionIfMissing(userServiceCollection, undefined, null);
        expect(expectedResult).toEqual(userServiceCollection);
      });
    });

    describe('compareUserService', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareUserService(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareUserService(entity1, entity2);
        const compareResult2 = service.compareUserService(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareUserService(entity1, entity2);
        const compareResult2 = service.compareUserService(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareUserService(entity1, entity2);
        const compareResult2 = service.compareUserService(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
