import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { ApiDetailComponent } from './api-detail.component';

describe('Api Management Detail Component', () => {
  let comp: ApiDetailComponent;
  let fixture: ComponentFixture<ApiDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: ApiDetailComponent,
              resolve: { api: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(ApiDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApiDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load api on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', ApiDetailComponent);

      // THEN
      expect(instance.api()).toEqual(expect.objectContaining({ id: 123 }));
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
