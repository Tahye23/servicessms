import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateCampagneComponent } from './create-campagne.component';

describe('CreateCampagneComponent', () => {
  let component: CreateCampagneComponent;
  let fixture: ComponentFixture<CreateCampagneComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateCampagneComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateCampagneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
