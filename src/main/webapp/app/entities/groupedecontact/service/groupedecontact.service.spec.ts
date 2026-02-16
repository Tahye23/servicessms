import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IGroupedecontact } from '../groupedecontact.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../groupedecontact.test-samples';

import { GroupedecontactService } from './groupedecontact.service';

const requireRestSample: IGroupedecontact = {
  ...sampleWithRequiredData,
};

describe('Groupedecontact Service', () => {
  let service: GroupedecontactService;
  let httpMock: HttpTestingController;
  let expectedResult: IGroupedecontact | IGroupedecontact[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(GroupedecontactService);
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

    it('should create a Groupedecontact', () => {
      const groupedecontact = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(groupedecontact).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Groupedecontact', () => {
      const groupedecontact = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(groupedecontact).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Groupedecontact', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Groupedecontact', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Groupedecontact', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addGroupedecontactToCollectionIfMissing', () => {
      it('should add a Groupedecontact to an empty array', () => {
        const groupedecontact: IGroupedecontact = sampleWithRequiredData;
        expectedResult = service.addGroupedecontactToCollectionIfMissing([], groupedecontact);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(groupedecontact);
      });

      it('should not add a Groupedecontact to an array that contains it', () => {
        const groupedecontact: IGroupedecontact = sampleWithRequiredData;
        const groupedecontactCollection: IGroupedecontact[] = [
          {
            ...groupedecontact,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addGroupedecontactToCollectionIfMissing(groupedecontactCollection, groupedecontact);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Groupedecontact to an array that doesn't contain it", () => {
        const groupedecontact: IGroupedecontact = sampleWithRequiredData;
        const groupedecontactCollection: IGroupedecontact[] = [sampleWithPartialData];
        expectedResult = service.addGroupedecontactToCollectionIfMissing(groupedecontactCollection, groupedecontact);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(groupedecontact);
      });

      it('should add only unique Groupedecontact to an array', () => {
        const groupedecontactArray: IGroupedecontact[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const groupedecontactCollection: IGroupedecontact[] = [sampleWithRequiredData];
        expectedResult = service.addGroupedecontactToCollectionIfMissing(groupedecontactCollection, ...groupedecontactArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const groupedecontact: IGroupedecontact = sampleWithRequiredData;
        const groupedecontact2: IGroupedecontact = sampleWithPartialData;
        expectedResult = service.addGroupedecontactToCollectionIfMissing([], groupedecontact, groupedecontact2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(groupedecontact);
        expect(expectedResult).toContain(groupedecontact2);
      });

      it('should accept null and undefined values', () => {
        const groupedecontact: IGroupedecontact = sampleWithRequiredData;
        expectedResult = service.addGroupedecontactToCollectionIfMissing([], null, groupedecontact, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(groupedecontact);
      });

      it('should return initial array if no Groupedecontact is added', () => {
        const groupedecontactCollection: IGroupedecontact[] = [sampleWithRequiredData];
        expectedResult = service.addGroupedecontactToCollectionIfMissing(groupedecontactCollection, undefined, null);
        expect(expectedResult).toEqual(groupedecontactCollection);
      });
    });

    describe('compareGroupedecontact', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareGroupedecontact(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareGroupedecontact(entity1, entity2);
        const compareResult2 = service.compareGroupedecontact(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareGroupedecontact(entity1, entity2);
        const compareResult2 = service.compareGroupedecontact(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareGroupedecontact(entity1, entity2);
        const compareResult2 = service.compareGroupedecontact(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
