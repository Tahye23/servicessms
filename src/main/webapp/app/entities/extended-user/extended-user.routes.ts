import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ExtendedUserComponent } from './list/extended-user.component';
import { ExtendedUserDetailComponent } from './detail/extended-user-detail.component';
import { ExtendedUserUpdateComponent } from './update/extended-user-update.component';
import ExtendedUserResolve from './route/extended-user-routing-resolve.service';

const extendedUserRoute: Routes = [
  {
    path: '',
    component: ExtendedUserComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ExtendedUserDetailComponent,
    resolve: {
      extendedUser: ExtendedUserResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ExtendedUserUpdateComponent,
    resolve: {
      extendedUser: ExtendedUserResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ExtendedUserUpdateComponent,
    resolve: {
      extendedUser: ExtendedUserResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default extendedUserRoute;
