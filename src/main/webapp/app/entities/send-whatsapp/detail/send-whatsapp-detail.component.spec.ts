import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SendWhatsappDetailComponent } from './send-whatsapp-detail.component';

describe('SendWhatsappDetailComponent', () => {
  let component: SendWhatsappDetailComponent;
  let fixture: ComponentFixture<SendWhatsappDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SendWhatsappDetailComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SendWhatsappDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
