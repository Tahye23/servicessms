import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { UserServiceComponent } from './list/user-service.component';
import { UserServiceDetailComponent } from './detail/user-service-detail.component';
import { UserServiceUpdateComponent } from './update/user-service-update.component';
import UserServiceResolve from './route/user-service-routing-resolve.service';

const userServiceRoute: Routes = [
  {
    path: '',
    component: UserServiceComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: UserServiceDetailComponent,
    resolve: {
      userService: UserServiceResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: UserServiceUpdateComponent,
    resolve: {
      userService: UserServiceResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: UserServiceUpdateComponent,
    resolve: {
      userService: UserServiceResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default userServiceRoute;
