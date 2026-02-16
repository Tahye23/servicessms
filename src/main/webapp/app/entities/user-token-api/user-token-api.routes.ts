import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { UserTokenApiComponent } from './list/user-token-api.component';
import { UserTokenApiDetailComponent } from './detail/user-token-api-detail.component';
import { UserTokenApiUpdateComponent } from './update/user-token-api-update.component';
import UserTokenApiResolve from './route/user-token-api-routing-resolve.service';

const userTokenApiRoute: Routes = [
  {
    path: '',
    component: UserTokenApiComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: UserTokenApiDetailComponent,
    resolve: {
      userTokenApi: UserTokenApiResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: UserTokenApiUpdateComponent,
    resolve: {
      userTokenApi: UserTokenApiResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: UserTokenApiUpdateComponent,
    resolve: {
      userTokenApi: UserTokenApiResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default userTokenApiRoute;
