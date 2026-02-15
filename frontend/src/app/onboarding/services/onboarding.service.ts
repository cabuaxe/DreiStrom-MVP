import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DecisionChoice,
  DecisionPointResponse,
  KurDecisionInput,
  KurDecisionResponse,
  OnboardingProgressResponse,
  StepResponse,
} from '../models/onboarding.model';

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/onboarding`;

  initialize(): Observable<OnboardingProgressResponse> {
    return this.http.post<OnboardingProgressResponse>(`${this.baseUrl}/initialize`, {});
  }

  getProgress(): Observable<OnboardingProgressResponse> {
    return this.http.get<OnboardingProgressResponse>(`${this.baseUrl}/progress`);
  }

  startStep(stepNumber: number): Observable<StepResponse> {
    return this.http.post<StepResponse>(`${this.baseUrl}/steps/${stepNumber}/start`, {});
  }

  completeStep(stepNumber: number): Observable<StepResponse> {
    return this.http.post<StepResponse>(`${this.baseUrl}/steps/${stepNumber}/complete`, {});
  }

  makeDecision(decisionPointId: number, choice: DecisionChoice): Observable<DecisionPointResponse> {
    return this.http.post<DecisionPointResponse>(
      `${this.baseUrl}/decisions/${decisionPointId}`,
      { choice },
    );
  }

  evaluateKleinunternehmer(input: KurDecisionInput): Observable<KurDecisionResponse> {
    return this.http.post<KurDecisionResponse>(
      `${this.baseUrl}/decisions/kleinunternehmer/evaluate`,
      input,
    );
  }

  evaluateKleinunternehmerFromData(year?: number): Observable<KurDecisionResponse> {
    const params: Record<string, string> = {};
    if (year) params['year'] = year.toString();
    return this.http.get<KurDecisionResponse>(
      `${this.baseUrl}/decisions/kleinunternehmer/evaluate`,
      { params },
    );
  }
}
