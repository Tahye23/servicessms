import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { QuestionComponent } from './list/question.component';
import { QuestionDetailComponent } from './detail/question-detail.component';
import { QuestionUpdateComponent } from './update/question-update.component';
import QuestionResolve from './route/question-routing-resolve.service';

const questionRoute: Routes = [
  {
    path: '',
    component: QuestionComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: QuestionDetailComponent,
    resolve: {
      question: QuestionResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: QuestionUpdateComponent,
    resolve: {
      question: QuestionResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: QuestionUpdateComponent,
    resolve: {
      question: QuestionResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default questionRoute;
