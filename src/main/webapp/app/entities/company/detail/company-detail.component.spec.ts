import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { CompanyDetailComponent } from './company-detail.component';

describe('Company Management Detail Component', () => {
  let comp: CompanyDetailComponent;
  let fixture: ComponentFixture<CompanyDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompanyDetailComponent],
      providers: [
        provideRouter(
          [
            {
              path: '**',
              component: CompanyDetailComponent,
              resolve: { company: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    })
      .overrideTemplate(CompanyDetailComponent, '')
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CompanyDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load company on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', CompanyDetailComponent);

      // THEN
      expect(instance.company()).toEqual(expect.objectContaining({ id: 123 }));
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
