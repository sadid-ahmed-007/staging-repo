import React from 'react';
import { Link } from 'react-router-dom';
import { Clock } from 'lucide-react';
import Card from '../../components/shared/Card';

const PendingApproval = () => {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Card className="p-10 flex flex-col items-center">
          <div className="bg-blue-100 p-4 rounded-full mb-6">
            <Clock className="w-12 h-12 text-blue-600" />
          </div>
          
          <h2 className="text-3xl font-extrabold text-gray-900 mb-2">
            Account Verified!
          </h2>
          
          <p className="text-gray-600 mb-8">
            Your email has been verified successfully. Your account is currently pending admin approval. We will notify you by email once your account has been approved and is ready to use.
          </p>
          
          <Link to="/login" className="text-primary-600 hover:text-primary-500 font-medium flex items-center">
            Back to Login
          </Link>
        </Card>
      </div>
    </div>
  );
};

export default PendingApproval;
