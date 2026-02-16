import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IQuestion } from '../question.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../question.test-samples';

import { QuestionService } from './question.service';

const requireRestSample: IQuestion = {
  ...sampleWithRequiredData,
};

describe('Question Service', () => {
  let service: QuestionService;
  let httpMock: HttpTestingController;
  let expectedResult: IQuestion | IQuestion[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(QuestionService);
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

    it('should create a Question', () => {
      const question = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(question).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Question', () => {
      const question = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(question).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Question', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Question', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Question', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addQuestionToCollectionIfMissing', () => {
      it('should add a Question to an empty array', () => {
        const question: IQuestion = sampleWithRequiredData;
        expectedResult = service.addQuestionToCollectionIfMissing([], question);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(question);
      });

      it('should not add a Question to an array that contains it', () => {
        const question: IQuestion = sampleWithRequiredData;
        const questionCollection: IQuestion[] = [
          {
            ...question,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addQuestionToCollectionIfMissing(questionCollection, question);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Question to an array that doesn't contain it", () => {
        const question: IQuestion = sampleWithRequiredData;
        const questionCollection: IQuestion[] = [sampleWithPartialData];
        expectedResult = service.addQuestionToCollectionIfMissing(questionCollection, question);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(question);
      });

      it('should add only unique Question to an array', () => {
        const questionArray: IQuestion[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const questionCollection: IQuestion[] = [sampleWithRequiredData];
        expectedResult = service.addQuestionToCollectionIfMissing(questionCollection, ...questionArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const question: IQuestion = sampleWithRequiredData;
        const question2: IQuestion = sampleWithPartialData;
        expectedResult = service.addQuestionToCollectionIfMissing([], question, question2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(question);
        expect(expectedResult).toContain(question2);
      });

      it('should accept null and undefined values', () => {
        const question: IQuestion = sampleWithRequiredData;
        expectedResult = service.addQuestionToCollectionIfMissing([], null, question, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(question);
      });

      it('should return initial array if no Question is added', () => {
        const questionCollection: IQuestion[] = [sampleWithRequiredData];
        expectedResult = service.addQuestionToCollectionIfMissing(questionCollection, undefined, null);
        expect(expectedResult).toEqual(questionCollection);
      });
    });

    describe('compareQuestion', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareQuestion(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareQuestion(entity1, entity2);
        const compareResult2 = service.compareQuestion(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareQuestion(entity1, entity2);
        const compareResult2 = service.compareQuestion(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareQuestion(entity1, entity2);
        const compareResult2 = service.compareQuestion(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
