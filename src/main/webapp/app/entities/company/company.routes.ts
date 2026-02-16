import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { CompanyComponent } from './list/company.component';
import { CompanyDetailComponent } from './detail/company-detail.component';
import { CompanyUpdateComponent } from './update/company-update.component';
import CompanyResolve from './route/company-routing-resolve.service';

const companyRoute: Routes = [
  {
    path: '',
    component: CompanyComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: CompanyDetailComponent,
    resolve: {
      company: CompanyResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: CompanyUpdateComponent,
    resolve: {
      company: CompanyResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: CompanyUpdateComponent,
    resolve: {
      company: CompanyResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default companyRoute;
