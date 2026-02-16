import { Directive, Input, TemplateRef, ViewContainerRef, inject, OnInit } from '@angular/core';
import { SubscriptionService } from './service/subscriptionService.service';

@Directive({
  selector: '[appFeatureAccess]',
  standalone: true,
})
export class FeatureAccessDirective implements OnInit {
  @Input() appFeatureAccess!: string;
  @Input() appFeatureAccessElse?: TemplateRef<any>;

  private templateRef = inject(TemplateRef<any>);
  private viewContainer = inject(ViewContainerRef);
  private subscriptionService = inject(SubscriptionService);

  ngOnInit() {
    this.subscriptionService.loadUserSubscriptions().subscribe(() => {
      this.updateView();
    });
  }

  private updateView() {
    const access = this.subscriptionService.checkFeatureAccess(this.appFeatureAccess);

    this.viewContainer.clear();

    if (access.allowed) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else if (this.appFeatureAccessElse) {
      this.viewContainer.createEmbeddedView(this.appFeatureAccessElse);
    }
  }
}
