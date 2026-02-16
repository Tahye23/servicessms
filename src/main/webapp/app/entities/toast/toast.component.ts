import { Component } from '@angular/core';
import { NgClass, NgIf } from '@angular/common';

@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css'],
  imports: [NgClass, NgIf],
  standalone: true,
})
export class ToastComponent {
  show = false;
  message = '';
  type: 'success' | 'error' | 'info' | 'warn' = 'success';

  showToast(message: string, type: 'success' | 'error' | 'info' | 'warn' = 'success', duration = 3000) {
    this.message = message;
    this.type = type;
    this.show = true;

    setTimeout(() => {
      this.show = false;
    }, duration);
  }

  get toastClasses() {
    switch (this.type) {
      case 'success':
        return 'bg-green-500 text-white';
      case 'error':
        return 'bg-red-500 text-white';
      case 'info':
        return 'bg-blue-500 text-white';
      case 'warn':
        return 'bg-yellow-500 text-white';
      default:
        return 'bg-gray-500 text-white';
    }
  }
}
