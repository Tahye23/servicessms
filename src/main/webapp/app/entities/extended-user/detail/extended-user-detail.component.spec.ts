import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { ExtendedUserDetailComponent } from './extended-user-detail.component';

describe('ExtendedUser Management Detail Component', () => {
  let comp: ExtendedUserDetailComponent;
  let fixture: ComponentFixture<ExtendedUserDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExtendedUserDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: ExtendedUserDetailComponent,
              resolve: { extendedUser: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(ExtendedUserDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ExtendedUserDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load extendedUser on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', ExtendedUserDetailComponent);

      // THEN
      expect(instance.extendedUser()).toEqual(expect.objectContaining({ id: 123 }));
    });
  });

  describe('PreviousState', () => {
    it('Should navigate to previous state', () => {
      jest.spyOn(window.history, 'back');
      comp.previousState();
      expect(window.history.back).toHaveBeenCalled();
    });
  });
});
