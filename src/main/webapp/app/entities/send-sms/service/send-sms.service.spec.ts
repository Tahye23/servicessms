import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ISendSms } from '../send-sms.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../send-sms.test-samples';

import { SendSmsService, RestSendSms } from './send-sms.service';

const requireRestSample: RestSendSms = {
  ...sampleWithRequiredData,
  sendateEnvoi: sampleWithRequiredData.sendateEnvoi?.toJSON(),
};

describe('SendSms Service', () => {
  let service: SendSmsService;
  let httpMock: HttpTestingController;
  let expectedResult: ISendSms | ISendSms[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(SendSmsService);
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

    it('should create a SendSms', () => {
      const sendSms = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(sendSms).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a SendSms', () => {
      const sendSms = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(sendSms).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a SendSms', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of SendSms', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a SendSms', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addSendSmsToCollectionIfMissing', () => {
      it('should add a SendSms to an empty array', () => {
        const sendSms: ISendSms = sampleWithRequiredData;
        expectedResult = service.addSendSmsToCollectionIfMissing([], sendSms);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(sendSms);
      });

      it('should not add a SendSms to an array that contains it', () => {
        const sendSms: ISendSms = sampleWithRequiredData;
        const sendSmsCollection: ISendSms[] = [
          {
            ...sendSms,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addSendSmsToCollectionIfMissing(sendSmsCollection, sendSms);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a SendSms to an array that doesn't contain it", () => {
        const sendSms: ISendSms = sampleWithRequiredData;
        const sendSmsCollection: ISendSms[] = [sampleWithPartialData];
        expectedResult = service.addSendSmsToCollectionIfMissing(sendSmsCollection, sendSms);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(sendSms);
      });

      it('should add only unique SendSms to an array', () => {
        const sendSmsArray: ISendSms[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const sendSmsCollection: ISendSms[] = [sampleWithRequiredData];
        expectedResult = service.addSendSmsToCollectionIfMissing(sendSmsCollection, ...sendSmsArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const sendSms: ISendSms = sampleWithRequiredData;
        const sendSms2: ISendSms = sampleWithPartialData;
        expectedResult = service.addSendSmsToCollectionIfMissing([], sendSms, sendSms2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(sendSms);
        expect(expectedResult).toContain(sendSms2);
      });

      it('should accept null and undefined values', () => {
        const sendSms: ISendSms = sampleWithRequiredData;
        expectedResult = service.addSendSmsToCollectionIfMissing([], null, sendSms, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(sendSms);
      });

      it('should return initial array if no SendSms is added', () => {
        const sendSmsCollection: ISendSms[] = [sampleWithRequiredData];
        expectedResult = service.addSendSmsToCollectionIfMissing(sendSmsCollection, undefined, null);
        expect(expectedResult).toEqual(sendSmsCollection);
      });
    });

    describe('compareSendSms', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareSendSms(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareSendSms(entity1, entity2);
        const compareResult2 = service.compareSendSms(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareSendSms(entity1, entity2);
        const compareResult2 = service.compareSendSms(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareSendSms(entity1, entity2);
        const compareResult2 = service.compareSendSms(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
