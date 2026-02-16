import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { FileextraitComponent } from './list/fileextrait.component';
import { FileextraitDetailComponent } from './detail/fileextrait-detail.component';
import { FileextraitUpdateComponent } from './update/fileextrait-update.component';
import FileextraitResolve from './route/fileextrait-routing-resolve.service';

const fileextraitRoute: Routes = [
  {
    path: '',
    component: FileextraitComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: FileextraitDetailComponent,
    resolve: {
      fileextrait: FileextraitResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: FileextraitUpdateComponent,
    resolve: {
      fileextrait: FileextraitResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: FileextraitUpdateComponent,
    resolve: {
      fileextrait: FileextraitResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default fileextraitRoute;
