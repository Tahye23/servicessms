jest.mock('@ng-bootstrap/ng-bootstrap');

import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { UserServiceService } from '../service/user-service.service';

import { UserServiceDeleteDialogComponent } from './user-service-delete-dialog.component';

describe('UserService Management Delete Component', () => {
  let comp: UserServiceDeleteDialogComponent;
  let fixture: ComponentFixture<UserServiceDeleteDialogComponent>;
  let service: UserServiceService;
  let mockActiveModal: NgbActiveModal;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, UserServiceDeleteDialogComponent],
      providers: [NgbActiveModal],
    })
      .overrideTemplate(UserServiceDeleteDialogComponent, '')
      .compileComponents();
    fixture = TestBed.createComponent(UserServiceDeleteDialogComponent);
    comp = fixture.componentInstance;
    service = TestBed.inject(UserServiceService);
    mockActiveModal = TestBed.inject(NgbActiveModal);
  });

  describe('confirmDelete', () => {
    it('Should call delete service on confirmDelete', inject(
      [],
      fakeAsync(() => {
        // GIVEN
        jest.spyOn(service, 'delete').mockReturnValue(of(new HttpResponse({ body: {} })));

        // WHEN
        comp.confirmDelete(123);
        tick();

        // THEN
        expect(service.delete).toHaveBeenCalledWith(123);
        expect(mockActiveModal.close).toHaveBeenCalledWith('deleted');
      }),
    ));

    it('Should not call delete service on clear', () => {
      // GIVEN
      jest.spyOn(service, 'delete');

      // WHEN
      comp.cancel();

      // THEN
      expect(service.delete).not.toHaveBeenCalled();
      expect(mockActiveModal.close).not.toHaveBeenCalled();
      expect(mockActiveModal.dismiss).toHaveBeenCalled();
    });
  });
});
