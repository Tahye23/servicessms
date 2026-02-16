import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IGroupe } from '../groupe.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../groupe.test-samples';

import { GroupeService } from './groupe.service';

const requireRestSample: IGroupe = {
  ...sampleWithRequiredData,
};

describe('Groupe Service', () => {
  let service: GroupeService;
  let httpMock: HttpTestingController;
  let expectedResult: IGroupe | IGroupe[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(GroupeService);
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

    it('should create a Groupe', () => {
      const groupe = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(groupe).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Groupe', () => {
      const groupe = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(groupe).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Groupe', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Groupe', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Groupe', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addGroupeToCollectionIfMissing', () => {
      it('should add a Groupe to an empty array', () => {
        const groupe: IGroupe = sampleWithRequiredData;
        expectedResult = service.addGroupeToCollectionIfMissing([], groupe);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(groupe);
      });

      it('should not add a Groupe to an array that contains it', () => {
        const groupe: IGroupe = sampleWithRequiredData;
        const groupeCollection: IGroupe[] = [
          {
            ...groupe,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addGroupeToCollectionIfMissing(groupeCollection, groupe);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Groupe to an array that doesn't contain it", () => {
        const groupe: IGroupe = sampleWithRequiredData;
        const groupeCollection: IGroupe[] = [sampleWithPartialData];
        expectedResult = service.addGroupeToCollectionIfMissing(groupeCollection, groupe);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(groupe);
      });

      it('should add only unique Groupe to an array', () => {
        const groupeArray: IGroupe[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const groupeCollection: IGroupe[] = [sampleWithRequiredData];
        expectedResult = service.addGroupeToCollectionIfMissing(groupeCollection, ...groupeArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const groupe: IGroupe = sampleWithRequiredData;
        const groupe2: IGroupe = sampleWithPartialData;
        expectedResult = service.addGroupeToCollectionIfMissing([], groupe, groupe2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(groupe);
        expect(expectedResult).toContain(groupe2);
      });

      it('should accept null and undefined values', () => {
        const groupe: IGroupe = sampleWithRequiredData;
        expectedResult = service.addGroupeToCollectionIfMissing([], null, groupe, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(groupe);
      });

      it('should return initial array if no Groupe is added', () => {
        const groupeCollection: IGroupe[] = [sampleWithRequiredData];
        expectedResult = service.addGroupeToCollectionIfMissing(groupeCollection, undefined, null);
        expect(expectedResult).toEqual(groupeCollection);
      });
    });

    describe('compareGroupe', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareGroupe(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareGroupe(entity1, entity2);
        const compareResult2 = service.compareGroupe(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareGroupe(entity1, entity2);
        const compareResult2 = service.compareGroupe(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareGroupe(entity1, entity2);
        const compareResult2 = service.compareGroupe(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
