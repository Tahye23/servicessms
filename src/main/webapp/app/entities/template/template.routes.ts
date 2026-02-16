import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TemplateListComponent } from './list/template-list.component';
import { TemplateCreateComponent } from './create/template-create.component';
import { TemplateDetailComponent } from './detail/template-detail.component';
import templateResolve from './routes/template-routing-resolve.service';
import { TemplateRendererComponent } from './detailMessage/template-renderer.component';
import { Authority } from '../../config/authority.constants';
import { FeatureAccessGuard } from '../../core/auth/featureAccessGuard.service';

const templateAppRoute: Routes = [
  {
    path: '',
    component: TemplateListComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },

  {
    path: ':id/view',
    component: TemplateDetailComponent,
    resolve: {
      tokensApp: templateResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'templates',
    },
  },
  {
    path: 'new',
    component: TemplateCreateComponent,
    resolve: {
      tokensApp: templateResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'templates',
      permission: 'templates.create',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
  },
  {
    path: ':id/edit',
    component: TemplateCreateComponent,
    resolve: {
      tokensApp: templateResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.PARTNER, Authority.USER],
      feature: 'templates',
      permission: 'templates.edit',
    },
    canActivate: [UserRouteAccessService, FeatureAccessGuard],
  },
];

export default templateAppRoute;
