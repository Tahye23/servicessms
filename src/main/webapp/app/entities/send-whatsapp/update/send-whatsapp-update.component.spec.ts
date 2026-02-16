import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SendWhatsappUpdateComponent } from './send-whatsapp-update.component';

describe('SendWhatsappUpdateComponent', () => {
  let component: SendWhatsappUpdateComponent;
  let fixture: ComponentFixture<SendWhatsappUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SendWhatsappUpdateComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SendWhatsappUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
